load_data <- function(path) { 
  files <- dir(path, pattern = '\\.csv', full.names = TRUE)
  tables <- lapply(files, read.csv, stringsAsFactors=F)
  do.call(rbind, tables)
}

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
  x["unattended"] <- 0
  x["tDFed"] <- 0
  x["dTotAcumulado"] <- 0
  x["tDTot"] <- 0
  #x["unattendedAcumulado"] <- 0
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
    unattended <- 0
    tDFed <- 0
    dTotAcumulado <- 0
    tDTot <- 0
    
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
	      tDFed <- tDFed + (x[j,]$t - x[j-1,]$t)
      }
      
      if(x[j-1,]$oFed>0){
        offered <- offered + (x[j-1,]$oFed * (x[j,]$t - x[j-1,]$t))
      }
      
      if(x[j-1,]$unat>0){
        unattended <- unattended + (x[j-1,]$unat * (x[j,]$t - x[j-1,]$t))
      }

      if(x[j-1,]$dTot>0){
	      dTotAcumulado <- dTotAcumulado + (x[j-1,]$dTot * (x[j,]$t - x[j-1,]$t))
  	    tDTot <- tDTot + (x[j,]$t - x[j-1,]$t)
      }
      
      
      x[j-1,]$prov <- prov
      x[j-1,]$cons <- cons
      x[j-1,]$req <- req
      x[j-1,]$offered <- offered
      x[j-1,]$unattended <- unattended
      x[j-1,]$tDFed <- tDFed
      x[j-1,]$dTotAcumulado <- dTotAcumulado
      x[j-1,]$tDTot <- tDTot
       
      if((x[j-1,]$unattended + cons)==0){
        x[j-1,]$compSatisfaction <- -1
      } else {
        x[j-1,]$compSatisfaction <- 1 - (x[j-1,]$unattended/(x[j-1,]$unattended + cons))
      }
      
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

pathBase <- "/home/eduardolfalcao/git/"  #lsd
path <- paste(pathBase,"fogbow-manager/experiments/data scripts r/done/",sep="")
path$exp <- paste(path,"40peers-20capacity/fixingSatisfaction/cycle",sep="")


adjust_data <- function(tempo_final, orderTime, cycle){
  
  path$orderTime <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
  
  contention <- 0.5

  path$sdnof <- paste(path$orderTime,"sdnof",sep="")
  path$sdnof <- paste(path$sdnof,"-05kappa-comCaronas/with60sBreaks/",sep="")
  print(path$sdnof)
  data.sdnof.05 <- load_data(path$sdnof)
  data.sdnof.05 <- create_columns(data.sdnof.05, FALSE, 20, cycle, contention)
  data.sdnof.05 <- compute_results(data.sdnof.05, tempo_final)

  data.sdnof.05
}


tempo_final = 86400

cycle = 10
orderTime = 10

data.comcaronas.kappa05.sdnof = adjust_data(tempo_final, orderTime, cycle)
path$orderTime <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
write.csv(data.comcaronas.kappa05.sdnof, file = paste(paste(path$orderTime,"resultados-sdnof-kappa05-comcaronas.csv",sep="")))


data.comcaronas.kappa05.sdnof = read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/resultados-sdnof-kappa05-comcaronas.csv", header=TRUE, sep=",")
data.comcaronas.kappa05.sdnof$nof = "SD"

adjust_data <- function(tempo_final, orderTime, cycle){
  
  path$orderTime <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
  
  contention <- 0.5
 
  path$fdnof <- paste(path$orderTime,"fdnof-",sep="")
  path$fdnof <- paste(path$fdnof,"05kappa-comCaronas/with60sBreaks/",sep="")
  print(path$fdnof)
  data.fdnof.05 <- load_data(path$fdnof)
  data.fdnof.05 <- create_columns(data.fdnof.05, TRUE, 20, cycle, contention)
  data.fdnof.05 <- compute_results(data.fdnof.05, tempo_final)
  
  data.fdnof.05
}

data.comcaronas.kappa05.fdnof = adjust_data(tempo_final, orderTime, cycle)

write.csv(data.comcaronas.kappa05.fdnof, file = paste(paste(path$orderTime,"resultados-fdnof-kappa05-comcaronas.csv",sep="")))

data.comcaronas.kappa05.fdnof = read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/resultados-fdnof-kappa05-comcaronas.csv", header=TRUE, sep=",")
data.comcaronas.kappa05.fdnof$nof = "FD"

data.caronas <- rbind(data.comcaronas.kappa05.sdnof,data.comcaronas.kappa05.fdnof)

library(stringi)
data.caronas$id <- stri_replace_all_regex(data.caronas$id, "p", "")
data.caronas$id <- sapply( data.caronas$id, as.numeric )

data.caronas$comportamento="cooperativo"
data.caronas[data.caronas$id<=40,]$comportamento <- "cooperativo"
data.caronas[data.caronas$id>40,]$comportamento <- "carona"

data.caronas$nof = factor(data.caronas$nof, levels=c('NoF: SD','NoF: FD'))

names(data.caronas)[names(data.caronas) == 'nof'] <- 'NoF'

head(data.caronas)

library(ggplot2)
library(dplyr)
data.caronas = data.caronas %>% mutate(nof_label = paste("NoF:", NoF))

path$cycle <- paste(paste(path$exp,10,sep=""),"/",sep="")
png(paste(path$cycle,"satisfactionComp-withfreeriders-sbrc.png",sep=""), width=400, height=350)
ggplot(data.caronas[data.caronas$t<tempo_final,], aes(t, compSatisfaction)) + 
  geom_line(aes(colour=comportamento, group=interaction(comportamento,id))) + facet_grid(. ~ nof) +
  theme_bw(base_size=15) + theme(legend.position = "right") + #scale_x_continuous(breaks = seq(0, tempo_final, by = 1200)) +
  scale_y_continuous(breaks = seq(0,1, by = 0.25), limits=c(0,1)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1), plot.title = element_text(hjust = 0.5)) +
  ylab("satisfação") + theme(legend.position = "top")
dev.off()


library(plyr)
library(reshape2)

datalambda <- data.caronas

datalambdaQuartiles.fair.sd <- ddply(datalambda[datalambda$t%%60==0 & datalambda$NoF=="SD" & datalambda$cycle==10 & datalambda$t<=tempo_final & datalambda$id<=40,], "t", summarise, máximo=max(fairness), terceiro_quartil=quantile(fairness, probs=.75),
                                     média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
datalambdaQuartiles.fair.melt.sd <- melt(datalambdaQuartiles.fair.sd, id.vars = "t")
datalambdaQuartiles.fair.melt.sd$NoF <- "SD"
datalambdaQuartiles.fair.melt.sd$cycle <- 10

datalambdaQuartiles.fair.fd <- ddply(datalambda[datalambda$t%%60==0 & datalambda$NoF=="FD" & datalambda$cycle==10 & datalambda$t<=tempo_final & datalambda$id<=40,], "t", summarise, máximo=max(fairness), terceiro_quartil=quantile(fairness, probs=.75),
                                     média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
datalambdaQuartiles.fair.melt.fd <- melt(datalambdaQuartiles.fair.fd, id.vars = "t")
datalambdaQuartiles.fair.melt.fd$NoF <- "FD"
datalambdaQuartiles.fair.melt.fd$cycle <- 10

# datalambdaQuartiles.fair.fdlambda <- ddply(datalambda[datalambda$t%%60==0 & datalambda$NoF=="fd" & datalambda$cycle==30 & datalambda$t<=tempo_final & datalambda$id<=40,], "t", summarise, máximo=max(fairness), terceiro_quartil=quantile(fairness, probs=.75),
#                                            média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
# datalambdaQuartiles.fair.melt.fdlambda <- melt(datalambdaQuartiles.fair.fdlambda, id.vars = "t")
# datalambdaQuartiles.fair.melt.fdlambda$NoF <- "fd"
# datalambdaQuartiles.fair.melt.fdlambda$cycle <- 30

datalambdaQuartiles.fair.melt = rbind(datalambdaQuartiles.fair.melt.sd,datalambdaQuartiles.fair.melt.fd)

datalambdaQuartiles.fair.melt$NoF = factor(datalambdaQuartiles.fair.melt$NoF, levels=c('SD','FD'))

datalambdaQuartiles.fair.melt = datalambdaQuartiles.fair.melt %>% mutate(nof_label = paste("NoF:", NoF))

datalambdaQuartiles.fair.melt$nof = factor(datalambdaQuartiles.fair.melt$nof, levels=c('NoF: SD','NoF: FD'))
#data.caronas$nof = factor(data.caronas$nof, levels=c('NoF: SD','NoF: FD'))

library(ggplot2)
path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
#png(paste(path$cycle,"fairness-withfreeriders.png",sep=""), width=700, height=400)
png(paste(path$cycle,"fairness-withfreeriders-sbrc2018.png",sep=""), width=400, height=350)
ggplot(datalambdaQuartiles.fair.melt, aes(x = t, y = value, color = variable)) +
  theme_bw(base_size=15) + #scale_x_continuous(breaks = seq(0, tempo_final, by = 600)) +
  scale_y_continuous(breaks = seq(0,3.5, by = 0.5), limits = c(0,3.5)) +
  xlim(0,86399) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) + theme(legend.position = "top") +
  facet_grid(. ~ nof) + ylab("paridade") + theme(legend.title=element_blank()) +
  geom_line()
dev.off()
