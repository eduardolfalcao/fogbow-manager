load_data <- function(path) { 
  files <- dir(path, pattern = '\\.csv', full.names = TRUE)
  tables <- lapply(files, read.csv, stringsAsFactors=F)
  do.call(rbind, tables)
}

create_columns <- function(x, controller, capacity, cycle){
  if(controller==TRUE){
    x["nof"] <- "fd"
  } else{
    x["nof"] <- "sd"
  }
  x["cap"] <- capacity
  x["prov"] <- 0
  x["cons"] <- 0
  x["offered"] <- 0
  x["req"] <- 0
  x["fairness"] <- 0
  x["satisfaction"] <- 0
  x["contention"] <- 0
  x["cycle"] <- cycle
  x
}

#install.packages("foreach")
#install.packages("doMC")
#install.packages("dplyr")

library(foreach)
library(doMC)
library(dplyr)
registerDoMC(cores = 7)

compute_results <- function(df_data,tempo_final){
  
  peers <- unique(df_data$id)
  
  result <- foreach(i = 1:length(peers), .combine = rbind) %dopar% {
    peer <- peers[i]
    
    x <- df_data %>% filter(id == peer)
    prov <- 0
    cons <- 0
    req <- 0
    offered <- 0
    
    finished <- FALSE
    
    for(j in 2:nrow(x)){
      if(x[j-1,]$sFed>0){
        prov <- prov + (x[j-1,]$sFed * (x[j,]$t - x[j-1,]$t))
      }
      if(x[j-1,]$rFed>0){
        cons <- cons + (x[j-1,]$rFed * (x[j,]$t - x[j-1,]$t))
      }
      if(x[j-1,]$dFed>0){
        req <- req + (x[j-1,]$dFed * (x[j,]$t - x[j-1,]$t))
      }
      if(x[j-1,]$oFed>0){
        offered <- offered + (x[j-1,]$oFed * (x[j,]$t - x[j-1,]$t))
      }
      
      x[j-1,]$prov <- prov
      x[j-1,]$cons <- cons
      x[j-1,]$req <- req
      x[j-1,]$offered <- offered
      
      if(x[j-1,]$prov==0){
        x[j-1,]$fairness <- -1
      } else {
        x[j-1,]$fairness <- cons/prov
      }
      
      
      if(x[j,]$t>=tempo_final && finished == FALSE){
        x[j,]$t <- tempo_final
        finished <- TRUE
      }else if(x[j,]$t>tempo_final && finished == TRUE){
        x[j,]$t <- -1
      }
      
      if(x[j-1,]$req==0){
        x[j-1,]$satisfaction <- -1
      } else{
        x[j-1,]$satisfaction <- cons/req
      }
    }
    x
  }
  result<-result[!(result$t==-1),]
  return(result)
}

pathBase <- "/home/eduardo/git/"  #notebook
#pathBase <- "/home/eduardolfalcao/git/"  #lsd
path <- paste(pathBase,"fogbow-manager/experiments/data scripts r/done/",sep="")
path$exp <- paste(path,"40peers-20capacity/weightedNof/cycle",sep="")


adjust_data <- function(tempo_final, cycle, experiment){
  
  path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
  
  path$sdnof <- paste(path$cycle,"sdnof-",sep="")
  path$sdnof <- paste(path$sdnof,experiment,sep="")
  path$sdnof <- paste(path$sdnof,"-05kappa-fr/with60sBreaks/",sep="")
  data.sdnof <- load_data(path$sdnof)
  print(path$sdnof)
  data.sdnof <- create_columns(data.sdnof, FALSE, 20, cycle)
  data.sdnof <- compute_results(data.sdnof, tempo_final)
  
  path$fdnof <- paste(path$cycle,"fdnof-",sep="")
  path$fdnof <- paste(path$fdnof,experiment,sep="")
  path$fdnof <- paste(path$fdnof,"-05kappa-fr/with60sBreaks/",sep="")
  print(path$fdnof)
  data.fdnof <- load_data(path$fdnof)
  data.fdnof <- create_columns(data.fdnof, TRUE, 20, cycle)
  data.fdnof <- compute_results(data.fdnof, tempo_final)
  
  data <- rbind(data.sdnof, data.fdnof)  
  
  data
}

movingAverage <- function(x, n=1, centered=FALSE) {
  
  if (centered) {
    before <- floor  ((n-1)/2)
    after  <- ceiling((n-1)/2)
  } else {
    before <- n-1
    after  <- 0
  }
  
  # Track the sum and count of number of non-NA items
  s     <- rep(0, length(x))
  count <- rep(0, length(x))
  
  # Add the centered data 
  new <- x
  # Add to count list wherever there isn't a 
  count <- count + !is.na(new)
  # Now replace NA_s with 0_s and add to total
  new[is.na(new)] <- 0
  s <- s + new
  
  # Add the data from before
  i <- 1
  while (i <= before) {
    # This is the vector with offset values to add
    new   <- c(rep(NA, i), x[1:(length(x)-i)])
    
    count <- count + !is.na(new)
    new[is.na(new)] <- 0
    s <- s + new
    
    i <- i+1
  }
  
  # Add the data from after
  i <- 1
  while (i <= after) {
    # This is the vector with offset values to add
    new   <- c(x[(i+1):length(x)], rep(NA, i))
    
    count <- count + !is.na(new)
    new[is.na(new)] <- 0
    s <- s + new
    
    i <- i+1
  }
  
  # return sum divided by count
  s/count
}

get_contention <- function(orderTime, experiment, cycle, ma=100){
  path$orderTime <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
  
  path$sdnof <- paste(path$orderTime,"sdnof-",sep="")
  path$sdnof <- paste(path$sdnof,experiment,sep="")
  path$sdnof <- paste(path$sdnof,"-05kappa/contention/",sep="")
  print(path$sdnof)
  data.sdnof <- load_data(path$sdnof)
  data.sdnof$orderTime <- orderTime
  data.sdnof$kappa <- movingAverage(data.sdnof$kappa,ma)
  
  path$fdnof <- paste(path$orderTime,"fdnof-",sep="")
  path$fdnof <- paste(path$fdnof,experiment,sep="")
  path$fdnof <- paste(path$fdnof,"-05kappa/contention/",sep="")
  print(path$fdnof)
  data.fdnof <- load_data(path$fdnof)
  data.fdnof$orderTime <- orderTime
  data.fdnof$kappa <- movingAverage(data.fdnof$kappa,ma)
  
  data <- rbind(data.sdnof, data.fdnof)
  
  data
}

tempo_final = 86400
cycle = 10
experiment <- paste(cycle,"minutes",sep="")
data.cycle10 = adjust_data(tempo_final, cycle, experiment)


adjust_data <- function(tempo_final, cycle, experiment){
  
  path$cycle <- paste(paste(path$exp,10,sep=""),"/",sep="")
  
  path$fdnof <- paste(path$cycle,"fdnof-",sep="")
  path$fdnof <- paste(path$fdnof,experiment,sep="")
  path$fdnof <- paste(path$fdnof,"-05kappa-fr/with60sBreaks/",sep="")
  print(path$fdnof)
  data.fdnof <- load_data(path$fdnof)
  data.fdnof <- create_columns(data.fdnof, TRUE, 20, cycle)
  data.fdnof <- compute_results(data.fdnof, tempo_final)
  
  data.fdnof
}

cycle = 30
experiment <- paste(cycle,"minutes",sep="")
data.cycle30 = adjust_data(tempo_final, cycle, experiment)

head(data.cycle10)

data.cycle10[data.cycle10$nof=="sd",]

data = rbind(data.cycle10,data.cycle30)









data.orderTime30$cycle <- 30
data.cycle10.contention = get_contention(orderTime,experiment, cycle,ma=1)

data.orderTime7 = data.10cycle

cycle = 30
data.30cycle = adjust_data(tempo_final, cycle)
data.30cycle.contention = get_contention(cycle)

cycle = 60
data.60cycle = adjust_data(tempo_final, cycle)
data.60cycle.contention = get_contention(cycle)

data = rbind(data.10cycle,data.30cycle,data.60cycle)

#install.packages("stringi")
library(stringi)
data$id <- stri_replace_all_regex(data$id, "p", "")
data$id <- sapply( data$id, as.numeric )

data$comportamento="cooperativo"
data[data$id<=40,]$comportamento <- "cooperativo"
data[data$id>40,]$comportamento <- "carona"

data$nof = factor(data$nof, levels=c('sd','fd'))

library(ggplot2)
path$cycle <- paste(paste(path$exp,10,sep=""),"/",sep="")
png(paste(path$cycle,"satisfaction-lambda10e30-ciclo10e30-demand30-wfr-line-24h.png",sep=""), width=800, height=400)
ggplot(data[data$t<tempo_final,], aes(t, satisfaction)) + 
  geom_line(aes(colour=comportamento, group=interaction(comportamento,id))) + facet_wrap(nof ~ cycle, ncol=3) +
  theme_bw(base_size=15) + theme(legend.position = "right") + #scale_x_continuous(breaks = seq(0, tempo_final, by = 1200)) +
  scale_y_continuous(breaks = seq(0,1.4, by = 0.25), limits=c(0,1.4)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1), plot.title = element_text(hjust = 0.5)) +
  ylab("satisfação") + theme(legend.position = "top")
dev.off()

#############################

#install.packages("ggplot2")
library(ggplot2)

head(data)

cycle <- 10
data <- data.orderTime10
path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")

png(paste(path$cycle,"fairness-lambda10-orderTime6.6.png",sep=""), width=350, height=400)
ggplot(data[data$t<=tempo_final,], aes(t, fairness)) + 
  geom_line(aes(colour=comportamento, group=interaction(comportamento,id))) +
  facet_grid(. ~ nof) +
  theme_bw(base_size=15) + theme(legend.position = "right") + #scale_x_continuous(breaks = seq(0, tempo_final, by = 1200)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1), plot.title = element_text(hjust = 0.5)) +
  ylab("justiça") + ylim(0,7) + theme(legend.position = "top")
  #ggtitle("order time = 6.6min, lambda = 10, 50 peers")
dev.off()

png(paste(path$cycle,"satisfaction-lambda10-orderTime6.6.png",sep=""), width=350, height=400)
ggplot(data[data$t<tempo_final,], aes(t, satisfaction)) + 
  geom_line(aes(colour=nof, group=interaction(nof,id))) +
  theme_bw(base_size=15) + theme(legend.position = "right") + #scale_x_continuous(breaks = seq(0, tempo_final, by = 1200)) +
  scale_y_continuous(breaks = seq(0,1.8, by = 0.25), limits=c(0,1.15)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1), plot.title = element_text(hjust = 0.5)) +
  ylab("satisfação") + theme(legend.position = "top")
  #ggtitle("order time = 6.6min, lambda = 10, 50 peers")
dev.off()

tempo_final=42000
data$comportamento="cooperativo"
data[data$id<=40,]$comportamento <- "cooperativo"
data[data$id>40,]$comportamento <- "carona"

data.orderTime30$comportamento="cooperativo"
library(stringi)
data.orderTime30$id <- stri_replace_all_regex(data.orderTime30$id, "p", "")
data.orderTime30$id <- sapply( data.orderTime30$id, as.numeric )
data.orderTime30[data.orderTime30$id<=40,]$comportamento <- "cooperativo"
data.orderTime30[data.orderTime30$id>40,]$comportamento <- "carona"

#data[data$t==36000 & data$satisfaction<=0.5 & data$nof=="fd",]$id
nrow(data[data$id<=40 & data$t==tempo_final-60 & data$nof=="fd" & data$fairness>=0.75,])
#data[data$id<=40 & data$id>=20 & data$t==tempo_final-60 & data$nof=="fd",]

orderBy(data[data$id=="p11" & data$t==86400 & data$nof=="fd",])

data[data$t==60000 & data$nof=="sd" & data$comportamento=="cooperativo",]


path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
png(paste(path$cycle,"satisfaction-lambda10-ciclo10-demand30-wfr-line-24h.png",sep=""), width=800, height=400)
ggplot(data[data$t<tempo_final,], aes(t, satisfaction)) + 
  geom_line(aes(colour=comportamento, group=interaction(comportamento,id))) + facet_grid(. ~ nof) +
  theme_bw(base_size=15) + theme(legend.position = "right") + #scale_x_continuous(breaks = seq(0, tempo_final, by = 1200)) +
  scale_y_continuous(breaks = seq(0,1.4, by = 0.25), limits=c(0,1.4)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1), plot.title = element_text(hjust = 0.5)) +
  ylab("satisfação") + theme(legend.position = "right")
dev.off()
#ggtitle("order time = 6.6min, lambda = 10, 50 peers")

path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
png(paste(path$cycle,"satisfaction-lambda10e30-ciclo10e30-demand30-wfr-line-24h.png",sep=""), width=800, height=400)
ggplot(datalambda[datalambda$t<tempo_final,], aes(t, satisfaction)) + 
  geom_line(aes(colour=comportamento, group=interaction(comportamento,id))) + facet_wrap(nof ~ cycle, ncol=3) +
  theme_bw(base_size=15) + theme(legend.position = "right") + #scale_x_continuous(breaks = seq(0, tempo_final, by = 1200)) +
  scale_y_continuous(breaks = seq(0,1.4, by = 0.25), limits=c(0,1.4)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1), plot.title = element_text(hjust = 0.5)) +
  ylab("satisfação") + theme(legend.position = "top")
dev.off()

datalambda$nof = factor(datalambda$nof, levels=c('sd','fd'))

data$nof = factor(data$nof, levels=c('sd','fd'))

png(paste(path$cycle,"fairness-lambda10-ciclo10-demand30-wfr-line.png",sep=""), width=800, height=400)
ggplot(data[data$t<tempo_final & data$id==11,], aes(t, fairness)) + 
  geom_line(aes(colour=comportamento, group=interaction(comportamento,id))) + facet_grid(. ~ nof) +
  theme_bw(base_size=15) + theme(legend.position = "right") + #scale_x_continuous(breaks = seq(0, tempo_final, by = 1200)) +
  scale_y_continuous(breaks = seq(-1,3.25, by = 0.25), limits=c(-1,3.25)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1), plot.title = element_text(hjust = 0.5)) +
  ylab("justiça") + theme(legend.position = "right")
dev.off()

#library(png)
#library(grid)
#library(gridExtra)
#img1 <-  rasterGrob(as.raster(readPNG(paste(path$cycle,"satisfaction-lambda10-orderTime6.6.png",sep=""))), interpolate = FALSE)
#img2 <-  rasterGrob(as.raster(readPNG(paste(path$cycle,"fairness-lambda10-orderTime6.6.png",sep=""))), interpolate = FALSE)

#png(paste(path$cycle,"lambda10-orderTime6.6.png",sep=""), width=800, height=450)
#grid.arrange(img1, img2, ncol = 2)
#dev.off()

data.cycle10.contention[data.cycle10.contention$nof=="sdnof-10minutes-0.5kappa",]$nof = "sd"
data.cycle10.contention[data.cycle10.contention$nof=="fdnof-10minutes-0.5kappa",]$nof = "fd"

png(paste(path$cycle,"contention--lambda10-orderTime10-demand30.png",sep=""), width=500, height=420)
ggplot(data.cycle10.contention[data.cycle10.contention$nof=="sd",], aes(t, kappa, colour=nof)) + 
  geom_line() + theme_bw(base_size=15) + theme(legend.position = "top") + #scale_x_continuous(breaks = seq(0, tempo_final, by = 600)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) + ylab(expression(kappa)) + 
  scale_color_manual(values=c("#00BFC4"))
dev.off()

ggplot(data.cycle10.contention[data.cycle10.contention$nof=="sd" & data.cycle10.contention$t>=599 & data.cycle10.contention$t<=635,], aes(t, kappa, colour=nof)) + 
  geom_line() + theme_bw(base_size=15) + theme(legend.position = "top") + #scale_x_continuous(breaks = seq(0, tempo_final, by = 600)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) + ylab(expression(kappa)) + 
  scale_color_manual(values=c("#00BFC4"))

png(paste(path$cycle,"contention-yLimitBy2.png",sep=""), width=1280, height=720)
ggplot(data.10cycle.contention, aes(t, kappa, colour=factor(orderTime))) + 
  geom_line() + theme_bw() + theme(legend.position = "top") + scale_x_continuous(breaks = seq(0, tempo_final, by = 50), limits = c(1800,3600)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) + ylim(0,5)# +xlim(1800,3600)
dev.off()

png(paste(path$cycle,"contention.png",sep=""), width=1280, height=720)
ggplot(data.30cycle.contention, aes(t, kappa, colour=nof)) + geom_line() + theme_bw() + theme(legend.position = "top") + scale_x_continuous(breaks = seq(0, tempo_final, by = 1800)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1))
dev.off()

png(paste(path$cycle,"contention.png",sep=""), width=1280, height=720)
ggplot(data.60cycle.contention, aes(t, kappa, colour=nof)) + geom_line() + theme_bw() + theme(legend.position = "top") + scale_x_continuous(breaks = seq(0, tempo_final, by = 3600)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) + ylim(0,5)
dev.off()



library(plyr)
library(reshape2)

dataQuartiles.sat.sd <- ddply(data[data$t%%60==0 & data$nof=="sd" & data$t<=42000,], "t", summarise, máximo=max(satisfaction), percentil_80=quantile(satisfaction, probs=.80),
                           média=mean(satisfaction), mediana=median(satisfaction), percentil_20=quantile(satisfaction, probs=.20), mínimo = min(satisfaction))
dataQuartiles.sat.melt.sd <- melt(dataQuartiles.sat.sd, id.vars = "t")
dataQuartiles.sat.melt.sd$nof <- "sd"

dataQuartiles.sat.fd <- ddply(data[data$t%%60==0 & data$nof=="fd" & data$t<=42000,], "t", summarise, máximo=max(satisfaction), percentil_80=quantile(satisfaction, probs=.80),
                              média=mean(satisfaction), mediana=median(satisfaction), percentil_20=quantile(satisfaction, probs=.20), mínimo = min(satisfaction))
dataQuartiles.sat.melt.fd <- melt(dataQuartiles.sat.fd, id.vars = "t")
dataQuartiles.sat.melt.fd$nof <- "fd"

dataQuartiles.sat.melt = rbind(dataQuartiles.sat.melt.sd,dataQuartiles.sat.melt.fd)

dataQuartiles.sat.melt$nof = factor(dataQuartiles.sat.melt$nof, levels=c('sd','fd'))

library(ggplot2)
path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
png(paste(path$cycle,"satisfacao-lambda10-ciclo10-demand30-wfr.png",sep=""), width=800, height=400)
ggplot(dataQuartiles.sat.melt, aes(x = t, y = value, color = variable)) +
  theme_bw(base_size=15) + #scale_x_continuous(breaks = seq(0, tempo_final, by = 600)) +
  scale_y_continuous(breaks = seq(0,1.5, by = 0.25), limits = c(0,1.5)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) +
  facet_grid(. ~ nof) + ylab("satisfação") + theme(legend.title=element_blank()) +
  geom_line()
dev.off()




library(plyr)
library(reshape2)
data[data$id<=40 & data$nof=="sd" & data$t>10000 & data$fairness>=3,]

data.orderTime30$cycle


data.orderTime30$nof <- "fd"
datalambda <- rbind(data,data.orderTime30)

datalambdaQuartiles.fair.sd <- ddply(datalambda[datalambda$t%%60==0 & datalambda$nof=="sd" & datalambda$cycle==10 & datalambda$t<=tempo_final & datalambda$id<=40,], "t", summarise, máximo=max(fairness), terceiro_quartil=quantile(fairness, probs=.75),
                              média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
datalambdaQuartiles.fair.melt.sd <- melt(datalambdaQuartiles.fair.sd, id.vars = "t")
datalambdaQuartiles.fair.melt.sd$nof <- "sd"
datalambdaQuartiles.fair.melt.sd$cycle <- 10

datalambdaQuartiles.fair.fd <- ddply(datalambda[datalambda$t%%60==0 & datalambda$nof=="fd" & datalambda$cycle==10 & datalambda$t<=tempo_final & datalambda$id<=40,], "t", summarise, máximo=max(fairness), terceiro_quartil=quantile(fairness, probs=.75),
                              média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
datalambdaQuartiles.fair.melt.fd <- melt(datalambdaQuartiles.fair.fd, id.vars = "t")
datalambdaQuartiles.fair.melt.fd$nof <- "fd"
datalambdaQuartiles.fair.melt.fd$cycle <- 10

datalambdaQuartiles.fair.fdlambda <- ddply(datalambda[datalambda$t%%60==0 & datalambda$nof=="fd" & datalambda$cycle==30 & datalambda$t<=tempo_final & datalambda$id<=40,], "t", summarise, máximo=max(fairness), terceiro_quartil=quantile(fairness, probs=.75),
                                     média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
datalambdaQuartiles.fair.melt.fdlambda <- melt(datalambdaQuartiles.fair.fdlambda, id.vars = "t")
datalambdaQuartiles.fair.melt.fdlambda$nof <- "fd"
datalambdaQuartiles.fair.melt.fdlambda$cycle <- 30

datalambdaQuartiles.fair.melt = rbind(datalambdaQuartiles.fair.melt.sd,datalambdaQuartiles.fair.melt.fd,datalambdaQuartiles.fair.melt.fdlambda)

datalambdaQuartiles.fair.melt$nof = factor(datalambdaQuartiles.fair.melt$nof, levels=c('sd','fd'))

library(ggplot2)
path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
png(paste(path$cycle,"fairness-lambda10e30-ciclo10e30-demand30-wfr-24h.png",sep=""), width=800, height=400)
ggplot(datalambdaQuartiles.fair.melt, aes(x = t, y = value, color = variable)) +
  theme_bw(base_size=15) + #scale_x_continuous(breaks = seq(0, tempo_final, by = 600)) +
  scale_y_continuous(breaks = seq(0,7.5, by = 0.5), limits = c(0,7.5)) +
  xlim(0,86399) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) + theme(legend.position = "top") +
  facet_wrap(nof ~ cycle, ncol=3) + ylab("justiça") + theme(legend.title=element_blank()) +
  geom_line()
dev.off()

dataQuartiles.fair.melt[dataQuartiles.fair.melt$value>=1.35 & dataQuartiles.fair.melt$t>40000 &
                          dataQuartiles.fair.melt$variable=="máximo" & dataQuartiles.fair.melt$nof=="fd",]

library(plyr)
dataQuartiles.sat <- ddply(data[data$orderTime==7 & data$t%%60==0,], "t", summarise, max=max(fairness), thirdquart=quantile(fairness, probs=.75),
                           mean=mean(fairness), median=median(fairness), firstquart=quantile(fairness, probs=.25), min = min(fairness))

library(reshape2)
dataQuartiles.sat.melt <- melt(dataQuartiles.sat, id.vars = "t")

library(ggplot2)
path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
png(paste(path$cycle,"fairness-sdnof-lambda7-quartiles.png",sep=""), width=1280, height=720)
ggplot(dataQuartiles.sat.melt, aes(x = t, y = value, color = variable)) +
  theme_bw() + scale_x_continuous(breaks = seq(0, tempo_final, by = 600)) +
  scale_y_continuous(breaks = seq(0,2, by = 0.25), limits = c(0,2)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) +
  geom_line()
dev.off()






summary(data[data$cycle==10 & data$nof=="sd" & data$t==42000 & data$orderTime==10,])

data[data$cycle==30 & data$nof=="sd" & data$t==42000 & data$satisfaction<0.017,]
#peer26
ggplot(data[data$cycle==30 & data$nof=="sd" & data$id==26,], aes(t, satisfaction)) + 
  geom_line(aes(colour=nof, group=interaction(nof,id))) +
  theme_bw() + theme(legend.position = "top") + scale_x_continuous(breaks = seq(0, tempo_final, by = 1800)) +
  ylim(0,1) + theme(axis.text.x = element_text(angle = 90, hjust = 1))





data[data$t>=1100 & data$t<=1300 & data$satisfaction<=0.5 & data$nof=="sd",]


png(paste(path$exp,"fairness-boxplots.png",sep=""), width=600, height=400)
ggplot(data[data$t==tempo_final,], aes(nof,fairness)) + ylim(0,2) +
  geom_boxplot(aes(colour=nof)) +
  theme_bw()
dev.off()

png(paste(path$exp,"satisfaction-boxplots.png",sep=""), width=600, height=400)
ggplot(data[data$t==tempo_final,], aes(nof,satisfaction)) + ylim(0,1) +
  geom_boxplot(aes(colour=nof)) +
  theme_bw()
dev.off()

png(paste(path$exp,"5NosComMaiorFairness-FairnessSDNOF.png",sep=""), width=800, height=400)
  ggplot(data[data$t==tempo_final & (data$id==25 | data$id==26 | data$id==52 | data$id==53 | data$id==56),], aes(nof, fairness)) + 
    facet_grid( . ~ id) + ylim(0,20) +
    geom_point(aes(colour=nof, group=interaction(nof,id))) +
    theme_bw() + theme(legend.position = "top") 
dev.off()


library(ggpubr)
greatestFairness.fairness = ggplot(data[data$t==tempo_final & (data$id==25 | data$id==26 | data$id==52 | data$id==53 | data$id==56),], aes(nof, fairness)) + 
  facet_grid( . ~ id) +
  geom_point(aes(colour=nof, group=interaction(nof,id))) +
  theme_bw() + theme(legend.position = "top") 
greatestFairness.satisfaction = ggplot(data[data$t==tempo_final & (data$id==25 | data$id==26 | data$id==52 | data$id==53 | data$id==56),], aes(nof, satisfaction)) + 
  facet_grid( . ~ id) +
  geom_point(aes(colour=nof, group=interaction(nof,id))) +
  theme_bw() + theme(legend.position = "top") 
png(paste(path$exp,"5NosComMaiorFairness.png",sep=""), width=800, height=800)
  ggarrange(greatestFairness.fairness, greatestFairness.satisfaction, ncol = 1, nrow = 2)
dev.off()

lowestFairness.fairness = ggplot(data[data$t==42000 & (data$id==18 | data$id==28 | data$id==35 | data$id==38 | data$id==50),], aes(nof, fairness)) + 
  facet_grid( . ~ id) +
  geom_point(aes(colour=nof, group=interaction(nof,id))) +
  theme_bw() + theme(legend.position = "top") 
lowestFairness.satisfaction = ggplot(data[data$t==42000 & (data$id==18 | data$id==28 | data$id==35 | data$id==38 | data$id==50),], aes(nof, satisfaction)) + 
  facet_grid( . ~ id) +
  geom_point(aes(colour=nof, group=interaction(nof,id))) +
  theme_bw() + theme(legend.position = "top") 
png(paste(path$done,"5NosComMenorFairness.png",sep=""), width=800, height=800)
  ggarrange(lowestFairness.fairness, lowestFairness.satisfaction, ncol = 1, nrow = 2)
dev.off()

summary(data[data$nof=="sd" & data$t==42000,])
summary(data[data$nof=="fd" & data$t==42000,])

nrow(data[data$nof=="sd" & data$t==42000,])
nrow(data[data$nof=="fd" & data$t==42000,])

#5 nos com maior fairness
data[data$nof=="sd" & data$t==42000 & data$fairness>=100,] # ou
data[data$nof=="fd" & data$t==42000 & (data$id==25 | data$id==26 | data$id==52 | data$id==53 | data$id==56),]

#5 nos com menor fairness
data[data$nof=="sd" & data$t==42000 & data$fairness<=0.008,] # ou
data[data$nof=="sd" & data$t==42000 & (data$id==18 | data$id==28 | data$id==35 | data$id==38 | data$id==50),]$fairness

data[data$nof=="fd" & data$t==42000 & (data$id==18 | data$id==28 | data$id==35 | data$id==38 | data$id==50),]$fairness





######################################
# analisando pela proporção oferta/demanda
reciprocalBehavior <- data.sdnof.13c[data.sdnof.13c$t==42000,]$req/data.sdnof.13c[data.sdnof.13c$t==42000,]$offered
#qplot(reciprocalBehavior, geom="histogram") + xlab("requested/offered, in the last moment of experiment")
#library(ggpubr)
#bp1 = ggplot(data = data.frame(y=reciprocalBehavior), aes(x = 1, y = y)) + geom_boxplot() + xlab("requested/offered, in the last moment of experiment")
#bp2 = ggplot(data = data.frame(y=reciprocalBehavior), aes(x = 1, y = y)) + geom_boxplot(outlier.shape = NA) + scale_y_continuous(0,50) + ylim(0,50) + xlab("zoom, y=[0,50]")
#bp3 = ggplot(data = data.frame(y=reciprocalBehavior), aes(x = 1, y = y)) + geom_boxplot(outlier.shape = NA) + scale_y_continuous(0,10) + ylim(0,10) + xlab("zoom, y=[0,10]")
#png(paste(path$done,"proporcaoDemandaOferta.png",sep=""), width=900, height=350)
#  ggarrange(bp1, bp2, bp3, ncol = 3, nrow = 1)
#dev.off()

summary(reciprocalBehavior)
dataTest = data.sdnof.13c[(data.sdnof.13c[data.sdnof.13c$t==42000,]$req/data.sdnof.13c[data.sdnof.13c$t==42000,]$offered)<=0.15060,]
library(stringi)
dataTest$id <- stri_replace_all_regex(dataTest$id, "p", "")
dataTest$id <- sapply( dataTest$id, as.numeric )

unique(dataTest$id)

nrow(data.sdnof.13c[(data.sdnof.13c[data.sdnof.13c$t==42000,]$req/data.sdnof.13c[data.sdnof.13c$t==42000,]$offered)<=0.15060 & data.sdnof.13c$t==42000,])

png(paste(path$done,"fairness-peersDo1QNapropDemandaOferta.png",sep=""), width=1280, height=720)
ggplot(data.sdnof.13c[(data.sdnof.13c[data.sdnof.13c$t==42000,]$req/data.sdnof.13c[data.sdnof.13c$t==42000,]$offered)<=0.15060,], aes(t, fairness)) + 
  geom_line(aes(colour=nof, group=interaction(nof,id))) +
  theme_bw() + theme(legend.position = "top") + ylim(0,100)
dev.off()

png(paste(path$done,"satisfaction.png",sep=""), width=1280, height=720)
ggplot(data, aes(t, satisfaction)) + 
  geom_line(aes(colour=nof, group=interaction(nof,id))) +
  theme_bw() + theme(legend.position = "top")
dev.off()


png(paste(path$done,"satisfaction-boxplots.png",sep=""), width=600, height=400)
ggplot(consumptionRate) + geom_boxplot() + theme_bw()
dev.off()



#################################33



head(data)


ggplot(data[,], aes(t, fairness)) + 
  #geom_line(aes(colour=nof)) +
  geom_line(aes(colour=nof, group=interaction(nof,id))) +
  theme_bw() + theme(legend.position = "top") +
  ylim(0,50)

teste = data[data$id=="p9" & data$t>=300 & data$t<=350,]

  
#theme(axis.text = element_text(size = 14), axis.title=element_text(size=14), legend.title = element_text(size = 13), legend.text=element_text(size=13), strip.text.y = element_text(size=13))



ggplot(data[data$t==7200,], aes(x = nof, y = fairness)) + ylim(0,6) + geom_boxplot()

summary(data[data$t==7200 & data$nof=="sd",])
summary(data[data$t==7200 & data$nof=="fd",])


ggplot(data[data$t==7200,], aes(fairness)) + stat_ecdf(geom = "step") + facet_grid(. ~ nof) + xlim(0,10)

library(plyr) # function ddply()
data.ecdf <- ddply(data[data$t==7200,], .(nof), transform, ecd = ecdf(fairness)(fairness))
ggplot(data.ecdf,aes(x = fairness, y = ecd)) + geom_line(aes(group = nof, colour = nof)) + xlim(0,10)

png("/home/eduardolfalcao/Dropbox/Doutorado/Disciplinas/Projeto de Tese 4/exp/+frWhitewashers/imgs/quali/+fr+allDeltas-sharingleveltotal-30rep-top.png", width=550, height=500)

ggplot(data, aes(t, fairness)) + 
  geom_line(aes(colour=id, group=interaction(nof,id))) +
  facet_grid(. ~ nof) +
  theme_bw() + ylab("fairness") + xlab("time") + theme(legend.position = "top") + ylim(0,20)
theme(axis.text = element_text(size = 14), axis.title=element_text(size=14), legend.title = element_text(size = 13), legend.text=element_text(size=13), strip.text.y = element_text(size=13))

dev.off()
#setwd(path$sdnof10C)


ggplot(data, aes(t, fairness)) + 
  geom_line(aes(colour=NoF, group=interaction(NoF,id))) +
  facet_grid(delta ~ ., labeller = label_bquote(Delta == .(x))) +
  scale_colour_discrete(name = "", labels = c("FD-NoF", "Transitive FD-NoF")) + 
  theme_bw() + ylab("nível de compartilhamento") + xlab("t") + theme(legend.position = "top") + scale_colour_grey(name="", start=0.75, end=0) +
  theme(axis.text = element_text(size = 14), axis.title=element_text(size=14), legend.title = element_text(size = 13), legend.text=element_text(size=13), strip.text.y = element_text(size=13))
dev.off()


# compute_results <- function(x){
#   prov <- 0
#   cons <- 0
#   req <- 0
#   peer <- ""
#   for(i in 2:length(x$id)){
#     if(!identical(peer, x[i,]$id)){
#       peer <- x[i,]$id
#       prov <- 0
#       cons <- 0
#       req <- 0
#     }
#     
#     print(paste(c(x[i,]$id, i), collapse = ", line="))
#     
#     if(x[i-1,]$sFed>0){
#       prov <- prov + (x[i-1,]$sFed * (x[i,]$t - x[i-1,]$t))  
#     }
#     if(x[i-1,]$rFed>0){
#       cons <- cons + (x[i-1,]$rFed * (x[i,]$t - x[i-1,]$t)) 
#     }
#     if(x[i-1,]$dFed>0){
#       req <- req + (x[i-1,]$dFed * (x[i,]$t - x[i-1,]$t))
#     }
#     x[i,]$prov <- prov
#     x[i,]$cons <- cons
#     x[i,]$req <- req
#     
#     if(x[i,]$prov==0){
#       x[i,]$fairness <- -1
#     } else {
#       x[i,]$fairness <- cons/prov
#     }
#     
#     if(x[i,]$req==0){
#       x[i,]$satisfaction <- -1
#     } else{
#       x[i,]$satisfaction <- cons/req
#     }
#   }
#   x
# }

##############################
library('igraph')
dat=read.csv("grafo2.csv",header=TRUE,row.names=1,check.names=FALSE)
m=as.matrix(dat)
g=graph.adjacency(m,mode="directed",weighted=TRUE)
plot(g)
