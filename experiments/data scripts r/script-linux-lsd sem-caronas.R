load_data <- function(path) { 
  files <- dir(path, pattern = '\\.csv', full.names = TRUE)
  tables <- lapply(files, read.csv, stringsAsFactors=F)
  do.call(rbind, tables)
}

#install.packages("foreach")
#install.packages("doMC")
#install.packages("dplyr")

library(foreach)
library(doMC)
library(dplyr)
registerDoMC(cores = 7)

create_columns <- function(x, controller, capacity, cycle, contention){
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
  x["compSatisfaction"] <- 0
  x["contention"] <- contention
  x["cycle"] <- cycle
  # x["unattended"] <- 0
  # x["unattendedAcumulado"] <- 0
  x
}

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
      if(x[j-1,]$oFed>0){
        offered <- offered + (x[j-1,]$oFed * (x[j,]$t - x[j-1,]$t))
      }
      
      x[j-1,]$prov <- prov
      x[j-1,]$cons <- cons
      x[j-1,]$req <- req
      x[j-1,]$offered <- offered
      
      # x[j-1,]$unattended <- (x[j-1,]$dTot - (x[j-1,]$dTot - max(x[j-1,]$dFed,x[j-1,]$rFed)) - x[j-1,]$rFed)*(x[j,]$t - x[j-1,]$t)
      # if(j > 2){
      #   x[j-1,]$unattendedAcumulado <- x[j-2,]$unattendedAcumulado + x[j-1,]$unattended
      # }else{
      #   x[j-1,]$unattendedAcumulado <- x[j-1,]$unattended
      # }
      # 
      # if((x[j-1,]$unattendedAcumulado + cons)==0){
      #   x[j-1,]$compSatisfaction <- -1
      # } else {
      #   x[j-1,]$compSatisfaction <- 1 - x[j-1,]$unattendedAcumulado/(x[j-1,]$unattendedAcumulado + cons)
      # }
      
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

#pathBase <- "/home/eduardo/git/"  #notebook
pathBase <- "/home/eduardolfalcao/git/"  #lsd
path <- paste(pathBase,"fogbow-manager/experiments/data scripts r/done/",sep="")
path$exp <- paste(path,"40peers-20capacity/weightedNof/cycle",sep="")


adjust_data <- function(tempo_final, orderTime, cycle){
  
  path$orderTime <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
  
  contention <- 0.5

  path$sdnof <- paste(path$orderTime,"sdnof",sep="")
  path$sdnof <- paste(path$sdnof,"-05kappa-semCaronas/with60sBreaks/",sep="")
  print(path$sdnof)
  data.sdnof.05 <- load_data(path$sdnof)
  data.sdnof.05 <- create_columns(data.sdnof.05, FALSE, 20, cycle, contention)
  data.sdnof.05 <- compute_results(data.sdnof.05, tempo_final)

  path$fdnof <- paste(path$orderTime,"fdnof-",sep="")
  path$fdnof <- paste(path$fdnof,"05kappa-semCaronas/with60sBreaks/",sep="")
  print(path$fdnof)
  data.fdnof.05 <- load_data(path$fdnof)
  data.fdnof.05 <- create_columns(data.fdnof.05, TRUE, 20, cycle, contention)
  data.fdnof.05 <- compute_results(data.fdnof.05, tempo_final)

  contention <- 1

  path$sdnof <- paste(path$orderTime,"sdnof",sep="")
  path$sdnof <- paste(path$sdnof,"-1kappa-semCaronas/with60sBreaks/",sep="")
  print(path$sdnof)
  data.sdnof.1 <- load_data(path$sdnof)
  data.sdnof.1 <- create_columns(data.sdnof.1, FALSE, 20, cycle, contention)
  data.sdnof.1 <- compute_results(data.sdnof.1, tempo_final)

  path$fdnof <- paste(path$orderTime,"fdnof-",sep="")
  path$fdnof <- paste(path$fdnof,"1kappa-semCaronas/with60sBreaks/",sep="")
  print(path$fdnof)
  data.fdnof.1 <- load_data(path$fdnof)
  data.fdnof.1 <- create_columns(data.fdnof.1, TRUE, 20, cycle, contention)
  data.fdnof.1 <- compute_results(data.fdnof.1, tempo_final)

  data <- rbind(data.sdnof.05, data.fdnof.05, data.sdnof.1, data.fdnof.1)

  data
}


tempo_final = 86400

cycle = 10
orderTime = 10
data.semcaronas = adjust_data(tempo_final, orderTime, cycle)
data = data.semcaronas

data.semcaronas[data.semcaronas$id=="p1" & data.semcaronas$satisfaction>1,]



## plotando a satisfação
library(plyr)
library(reshape2)

dataQuartiles.sat.sd.05 <- ddply(data[data$t%%60==0 & data$nof=="sd" & data$contention==0.5,], "t", summarise, terceiro_quartil=quantile(satisfaction, probs=.75),
                                 média=mean(satisfaction), mediana=median(satisfaction), primeiro_quartil=quantile(satisfaction, probs=.25), mínimo = min(satisfaction))
dataQuartiles.sat.melt.sd.05 <- melt(dataQuartiles.sat.sd.05, id.vars = "t")
dataQuartiles.sat.melt.sd.05$nof <- "sd"
dataQuartiles.sat.melt.sd.05$contenção <- 0.5

dataQuartiles.sat.fd.05 <- ddply(data[data$t%%60==0 & data$nof=="fd" & data$contention==0.5,], "t", summarise, terceiro_quartil=quantile(satisfaction, probs=.75),
                                 média=mean(satisfaction), mediana=median(satisfaction), primeiro_quartil=quantile(satisfaction, probs=.25), mínimo = min(satisfaction))
dataQuartiles.sat.melt.fd.05 <- melt(dataQuartiles.sat.fd.05, id.vars = "t")
dataQuartiles.sat.melt.fd.05$nof <- "fd"
dataQuartiles.sat.melt.fd.05$contenção <- 0.5

dataQuartiles.sat.sd.1 <- ddply(data[data$t%%60==0 & data$nof=="sd" & data$contention==1,], "t", summarise, terceiro_quartil=quantile(satisfaction, probs=.75),
                                média=mean(satisfaction), mediana=median(satisfaction), primeiro_quartil=quantile(satisfaction, probs=.25), mínimo = min(satisfaction))
dataQuartiles.sat.melt.sd.1 <- melt(dataQuartiles.sat.sd.1, id.vars = "t")
dataQuartiles.sat.melt.sd.1$nof <- "sd"
dataQuartiles.sat.melt.sd.1$contenção <- 1

dataQuartiles.sat.fd.1 <- ddply(data[data$t%%60==0 & data$nof=="fd" & data$contention==1,], "t", summarise, terceiro_quartil=quantile(satisfaction, probs=.75),
                                média=mean(satisfaction), mediana=median(satisfaction), primeiro_quartil=quantile(satisfaction, probs=.25), mínimo = min(satisfaction))
dataQuartiles.sat.melt.fd.1 <- melt(dataQuartiles.sat.fd.1, id.vars = "t")
dataQuartiles.sat.melt.fd.1$nof <- "fd"
dataQuartiles.sat.melt.fd.1$contenção <- 1

dataQuartiles.sat.melt = rbind(dataQuartiles.sat.melt.sd.05,dataQuartiles.sat.melt.fd.05,dataQuartiles.sat.melt.sd.1,dataQuartiles.sat.melt.fd.1)

dataQuartiles.sat.melt$nof = factor(dataQuartiles.sat.melt$nof, levels=c('sd','fd'))

library(ggplot2)
path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
png(paste(path$cycle,"satisfacao-lambda10-semcarona-kappa05-vert-3q.png",sep=""), width=800, height=350)
ggplot(dataQuartiles.sat.melt[dataQuartiles.sat.melt$t<tempo_final,], aes(x = t, y = value, color = variable)) +
  theme_bw(base_size=15) + #scale_x_continuous(breaks = seq(0, tempo_final, by = 600)) +
  scale_y_continuous(breaks = seq(0,1, by = 0.25), limits = c(0,1)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) +
  facet_wrap(contenção ~ nof, ncol=4, labeller = label_both) +
  #facet_grid(contenção ~ nof, labeller = label_both) + 
  ylab("satisfação") + theme(legend.title=element_blank()) + theme(legend.position = "top") +
  geom_line() + scale_colour_manual(values=c("#B79F00", "#00BA38", "#00BFC4", "#619CFF", "#F564E3")) 
dev.off()

library(scales)
show_col(hue_pal()(6))

summary(data.semcaronas[data.semcaronas$contention==1 & data.semcaronas$t==tempo_final-60 & data.semcaronas$nof=="sd",])
summary(data.semcaronas[data.semcaronas$contention==1 & data.semcaronas$t==tempo_final-60 & data.semcaronas$nof=="fd",])

nrow(data.semcaronas[data.semcaronas$contention==1 & data.semcaronas$t==tempo_final-60 & data.semcaronas$nof=="sd" & data.semcaronas$fairness>0.70,])
nrow(data.semcaronas[data.semcaronas$contention==1 & data.semcaronas$t==tempo_final-60 & data.semcaronas$nof=="fd" & data.semcaronas$fairness>0.70,])


###########
library(plyr)
library(reshape2)

dataQuartiles.fair.sd.05 <- ddply(data[data$t%%60==0 & data$nof=="sd" & data$contention==0.5,], "t", summarise, máximo=max(fairness),terceiro_quartil=quantile(fairness, probs=.75),
                                 média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
dataQuartiles.fair.melt.sd.05 <- melt(dataQuartiles.fair.sd.05, id.vars = "t")
dataQuartiles.fair.melt.sd.05$nof <- "sd"
dataQuartiles.fair.melt.sd.05$contenção <- 0.5

dataQuartiles.fair.fd.05 <- ddply(data[data$t%%60==0 & data$nof=="fd" & data$contention==0.5,], "t", summarise, máximo=max(fairness),terceiro_quartil=quantile(fairness, probs=.75),
                                 média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
dataQuartiles.fair.melt.fd.05 <- melt(dataQuartiles.fair.fd.05, id.vars = "t")
dataQuartiles.fair.melt.fd.05$nof <- "fd"
dataQuartiles.fair.melt.fd.05$contenção <- 0.5

dataQuartiles.fair.sd.1 <- ddply(data[data$t%%60==0 & data$nof=="sd" & data$contention==1,], "t", summarise, máximo=max(fairness),terceiro_quartil=quantile(fairness, probs=0.75),
                                média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
dataQuartiles.fair.melt.sd.1 <- melt(dataQuartiles.fair.sd.1, id.vars = "t")
dataQuartiles.fair.melt.sd.1$nof <- "sd"
dataQuartiles.fair.melt.sd.1$contenção <- 1

dataQuartiles.fair.fd.1 <- ddply(data[data$t%%60==0 & data$nof=="fd" & data$contention==1,], "t", summarise, máximo=max(fairness),terceiro_quartil=quantile(fairness, probs=.75),
                                média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
dataQuartiles.fair.melt.fd.1 <- melt(dataQuartiles.fair.fd.1, id.vars = "t")
dataQuartiles.fair.melt.fd.1$nof <- "fd"
dataQuartiles.fair.melt.fd.1$contenção <- 1

dataQuartiles.fair.melt = rbind(dataQuartiles.fair.melt.sd.05,dataQuartiles.fair.melt.fd.05,dataQuartiles.fair.melt.sd.1,dataQuartiles.fair.melt.fd.1)

dataQuartiles.fair.melt$nof = factor(dataQuartiles.fair.melt$nof, levels=c('sd','fd'))

library(ggplot2)
path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
png(paste(path$cycle,"fairness-lambda10-semcarona-kappa05e1-vert.png",sep=""), width=800, height=350)
ggplot(dataQuartiles.fair.melt[dataQuartiles.fair.melt$t<tempo_final,], aes(x = t, y = value, color = variable)) +
  theme_bw(base_size=15) + #scale_x_continuous(breaks = seq(0, tempo_final, by = 600)) +
  scale_y_continuous(breaks = seq(0,7, by = 0.5), limits = c(0,7)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) +
  facet_wrap(contenção ~ nof, ncol=4, labeller = label_both) +
  #facet_grid(contenção ~ nof, labeller = label_both) + 
  ylab("justiça") + theme(legend.title=element_blank()) + theme(legend.position = "top") +
  geom_line()
dev.off()








###################



data.05kappa.semcaronas.satComp = adjust_data(tempo_final, orderTime, cycle)
summary(data.05kappa.semcaronas.satComp[data.05kappa.semcaronas.satComp$t>80000,]$compSatisfaction)


head(data.orderTime10.05kappa)
data.orderTime10.05kappa$contenção = 0.5
summary(data.orderTime10.05kappa)



adjust_data <- function(tempo_final, orderTime, experiment, cycle){
  
  path$orderTime <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
  
  path$sdnof <- paste(path$orderTime,"sdnof-",sep="")
  path$sdnof <- paste(path$sdnof,experiment,sep="")
  path$sdnof <- paste(path$sdnof,"-1kappa/with60sBreaks/",sep="")
  print(path$sdnof)
  data.sdnof <- load_data(path$sdnof)
  data.sdnof <- create_columns(data.sdnof, FALSE, 20, cycle)
  data.sdnof <- compute_results(data.sdnof, tempo_final)
  data.sdnof$orderTime <- orderTime
  
  path$fdnof <- paste(path$orderTime,"fdnof-",sep="")
  path$fdnof <- paste(path$fdnof,experiment,sep="")
  path$fdnof <- paste(path$fdnof,"-1kappa/with60sBreaks/",sep="")
  print(path$fdnof)
  data.fdnof <- load_data(path$fdnof)
  data.fdnof <- create_columns(data.fdnof, TRUE, 20, cycle)
  data.fdnof <- compute_results(data.fdnof, tempo_final)
  data.fdnof$orderTime <- orderTime
  
  data <- rbind(data.sdnof, data.fdnof)  
  
  data
}

data.orderTime10.1kappa = adjust_data(tempo_final, orderTime, experiment, cycle)
data.orderTime10.1kappa$contenção=1




data = rbind(data.orderTime10.05kappa,data.orderTime10.1kappa)


#install.packages("stringi")
#library(stringi)
#data$id <- stri_replace_all_regex(data$id, "p", "")
#data$id <- sapply( data$id, as.numeric )

#############################

#install.packages("ggplot2")
library(ggplot2)

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
data[data$id<=40 & data$nof=="sd" & data$t>10000 & data$fairness>=3,]

data.orderTime30$cycle


data.orderTime30$nof <- "fd"
datalambda <- rbind(data,data.orderTime30)

datalambdaQuartiles.fair.sd.05 <- ddply(data[data$t%%60==0 & data$nof=="sd" & data$contenção==0.5,], "t", summarise, máximo=max(fairness), terceiro_quartil=quantile(fairness, probs=.75),
                              média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
datalambdaQuartiles.fair.melt.sd.05 <- melt(datalambdaQuartiles.fair.sd.05, id.vars = "t")
datalambdaQuartiles.fair.melt.sd.05$nof <- "sd"
datalambdaQuartiles.fair.melt.sd.05$cycle <- 10
datalambdaQuartiles.fair.melt.sd.05$contenção <- 0.5

datalambdaQuartiles.fair.fd.05 <- ddply(data[data$t%%60==0 & data$nof=="fd" & data$contenção==0.5,], "t", summarise, máximo=max(fairness), terceiro_quartil=quantile(fairness, probs=.75),
                              média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
datalambdaQuartiles.fair.melt.fd.05 <- melt(datalambdaQuartiles.fair.fd.05, id.vars = "t")
datalambdaQuartiles.fair.melt.fd.05$nof <- "fd"
datalambdaQuartiles.fair.melt.fd.05$cycle <- 10
datalambdaQuartiles.fair.melt.fd.05$contenção <- 0.5

datalambdaQuartiles.fair.sd.1 <- ddply(data[data$t%%60==0 & data$nof=="sd" & data$contenção==1,], "t", summarise, máximo=max(fairness), terceiro_quartil=quantile(fairness, probs=.75),
                                        média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
datalambdaQuartiles.fair.melt.sd.1 <- melt(datalambdaQuartiles.fair.sd.05, id.vars = "t")
datalambdaQuartiles.fair.melt.sd.1$nof <- "sd"
datalambdaQuartiles.fair.melt.sd.1$cycle <- 10
datalambdaQuartiles.fair.melt.sd.1$contenção <- 1

datalambdaQuartiles.fair.fd.1 <- ddply(data[data$t%%60==0 & data$nof=="fd" & data$contenção==1,], "t", summarise, máximo=max(fairness), terceiro_quartil=quantile(fairness, probs=.75),
                                        média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
datalambdaQuartiles.fair.melt.fd.1 <- melt(datalambdaQuartiles.fair.fd.1, id.vars = "t")
datalambdaQuartiles.fair.melt.fd.1$nof <- "fd"
datalambdaQuartiles.fair.melt.fd.1$cycle <- 10
datalambdaQuartiles.fair.melt.fd.1$contenção <- 1

datalambdaQuartiles.fair.melt = rbind(datalambdaQuartiles.fair.melt.sd.05,datalambdaQuartiles.fair.melt.fd.05,datalambdaQuartiles.fair.melt.sd.1,datalambdaQuartiles.fair.melt.fd.1)

datalambdaQuartiles.fair.melt$nof = factor(datalambdaQuartiles.fair.melt$nof, levels=c('sd','fd'))

library(ggplot2)
path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
png(paste(path$cycle,"fairness-lambda10-ciclo10-kappa05e1-semcarona.png",sep=""), width=800, height=600)
ggplot(datalambdaQuartiles.fair.melt, aes(x = t, y = value, color = variable)) +
  theme_bw(base_size=15) + #scale_x_continuous(breaks = seq(0, tempo_final, by = 600)) +
  scale_y_continuous(breaks = seq(0,7.5, by = 0.5), limits = c(0,7.5)) +
  xlim(0,86399) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) + theme(legend.position = "right") +
  facet_grid(contenção ~ nof, labeller = label_both) + ylab("justiça") + theme(legend.title=element_blank()) +
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
