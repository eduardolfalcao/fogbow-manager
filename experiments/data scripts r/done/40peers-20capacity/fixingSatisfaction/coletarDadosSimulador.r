load_data <- function(path) { 
  files <- dir(path, pattern = '\\.csv', full.names = TRUE)
  tables <- lapply(files, read.csv, stringsAsFactors=F)
  do.call(rbind, tables)
}

library(foreach)
library(doMC)
library(dplyr)
registerDoMC(cores = 8)

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

#pathBase <- "/home/eduardo/git/"  #notebook
pathBase <- "/home/eduardolfalcao/git/"  #lsd
path <- paste(pathBase,"fogbow-manager/experiments/data scripts r/done/",sep="")
path$exp <- paste(path,"40peers-20capacity/fixingSatisfaction/cycle",sep="")


adjust_data <- function(tempo_final, orderTime, cycle){
  
  path$orderTime <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
  
  contention <- 0.5

  path$sdnof <- paste(path$orderTime,"sdnof",sep="")
  path$sdnof <- paste(path$sdnof,"-05kappa-semCaronas18-01/with60sBreaks/",sep="")
  print(path$sdnof)
  data.sdnof.05 <- load_data(path$sdnof)
  data.sdnof.05 <- create_columns(data.sdnof.05, FALSE, 20, cycle, contention)
  data.sdnof.05 <- compute_results(data.sdnof.05, tempo_final)

  # path$fdnof <- paste(path$orderTime,"fdnof-",sep="")
  # path$fdnof <- paste(path$fdnof,"05kappa-semCaronas18-01/with60sBreaks/",sep="")
  # print(path$fdnof)
  # data.fdnof.05 <- load_data(path$fdnof)
  # data.fdnof.05 <- create_columns(data.fdnof.05, TRUE, 20, cycle, contention)
  # data.fdnof.05 <- compute_results(data.fdnof.05, tempo_final)

  # contention <- 1
  # 
  # path$sdnof <- paste(path$orderTime,"sdnof",sep="")
  # path$sdnof <- paste(path$sdnof,"-1kappa-semCaronas/with60sBreaks/",sep="")
  # print(path$sdnof)
  # data.sdnof.1 <- load_data(path$sdnof)
  # data.sdnof.1 <- create_columns(data.sdnof.1, FALSE, 20, cycle, contention)
  # data.sdnof.1 <- compute_results(data.sdnof.1, tempo_final)
  # 
  # path$fdnof <- paste(path$orderTime,"fdnof-",sep="")
  # path$fdnof <- paste(path$fdnof,"1kappa-semCaronas/with60sBreaks/",sep="")
  # print(path$fdnof)
  # data.fdnof.1 <- load_data(path$fdnof)
  # data.fdnof.1 <- create_columns(data.fdnof.1, TRUE, 20, cycle, contention)
  # data.fdnof.1 <- compute_results(data.fdnof.1, tempo_final)
  # 
  # data <- rbind(data.sdnof.05, data.fdnof.05, data.sdnof.1, data.fdnof.1)
  # data <- rbind(data.sdnof.05, data.fdnof.05)

  data.sdnof.05
  
  # data
}


tempo_final = 86400

cycle = 10
orderTime = 10

data.semcaronas.kappa05.sdnof.satisfaction = adjust_data(tempo_final, orderTime, cycle)

data.semcaronas.kappa05 = adjust_data(tempo_final, orderTime, cycle)

path$orderTime <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
write.csv(data.semcaronas.kappa05, file = paste(paste(path$orderTime,"resultados-kappa05-semcaronas.csv",sep="")))

data.semcaronas.kappa1.sdnof = adjust_data(tempo_final, orderTime, cycle)
write.csv(data.semcaronas.kappa05, file = paste(paste(path$orderTime,"resultados-sdnof-kappa1-semcaronas.csv",sep="")))
write.csv(data.semcaronas.kappa1.sdnof, file = paste(paste(path$orderTime,"resultados-sdnof-kappa1-v2-semcaronas.csv",sep="")))


adjust_data <- function(tempo_final, orderTime, cycle){
  
  path$orderTime <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
  
  # contention <- 0.5
  # 
  # path$sdnof <- paste(path$orderTime,"sdnof",sep="")
  # path$sdnof <- paste(path$sdnof,"-05kappa-semCaronas18-01/with60sBreaks/",sep="")
  # print(path$sdnof)
  # data.sdnof.05 <- load_data(path$sdnof)
  # data.sdnof.05 <- create_columns(data.sdnof.05, FALSE, 20, cycle, contention)
  # data.sdnof.05 <- compute_results(data.sdnof.05, tempo_final)
  # 
  # path$fdnof <- paste(path$orderTime,"fdnof-",sep="")
  # path$fdnof <- paste(path$fdnof,"05kappa-semCaronas18-01/with60sBreaks/",sep="")
  # print(path$fdnof)
  # data.fdnof.05 <- load_data(path$fdnof)
  # data.fdnof.05 <- create_columns(data.fdnof.05, TRUE, 20, cycle, contention)
  # data.fdnof.05 <- compute_results(data.fdnof.05, tempo_final)
  
  contention <- 1
  
  # path$sdnof <- paste(path$orderTime,"sdnof",sep="")
  # path$sdnof <- paste(path$sdnof,"-1kappa-semCaronas/with60sBreaks/",sep="")
  # print(path$sdnof)
  # data.sdnof.1 <- load_data(path$sdnof)
  # data.sdnof.1 <- create_columns(data.sdnof.1, FALSE, 20, cycle, contention)
  # data.sdnof.1 <- compute_results(data.sdnof.1, tempo_final)

  path$fdnof <- paste(path$orderTime,"fdnof-",sep="")
  path$fdnof <- paste(path$fdnof,"1kappa-semCaronas/with60sBreaks/",sep="")
  print(path$fdnof)
  data.fdnof.1 <- load_data(path$fdnof)
  data.fdnof.1 <- create_columns(data.fdnof.1, TRUE, 20, cycle, contention)
  data.fdnof.1 <- compute_results(data.fdnof.1, tempo_final)

  # data <- rbind(data.sdnof.05, data.fdnof.05, data.sdnof.1, data.fdnof.1)
  # data <- rbind(data.sdnof.05, data.fdnof.05)
  
  data.fdnof.1
  
  # data
}

data.semcaronas.kappa1.fdnof = adjust_data(tempo_final, orderTime, cycle)
write.csv(data.semcaronas.kappa1.fdnof, file = paste(paste(path$orderTime,"resultados-fdnof-kappa1-semcaronas.csv",sep="")))

data = data.semcaronas




## plotando a satisfação
library(plyr)
library(reshape2)

data = rbind(data.semcaronas.kappa05,data.semcaronas.kappa1.sdnof,data.semcaronas.kappa1.fdnof)

dataQuartiles.sat.sd.05 <- ddply(data[data$t%%60==0 & data$nof=="sd" & data$contention==0.5,], "t", summarise, máximo = max(compSatisfaction), terceiro_quartil=quantile(compSatisfaction, probs=.75),
                                 média=mean(compSatisfaction), mediana=median(compSatisfaction), primeiro_quartil=quantile(compSatisfaction, probs=.25), mínimo = min(compSatisfaction))
dataQuartiles.sat.melt.sd.05 <- melt(dataQuartiles.sat.sd.05, id.vars = "t")
dataQuartiles.sat.melt.sd.05$nof <- "SD"
dataQuartiles.sat.melt.sd.05$contenção <- 0.5

dataQuartiles.sat.fd.05 <- ddply(data[data$t%%60==0 & data$nof=="fd" & data$contention==0.5,], "t", summarise, máximo = max(compSatisfaction), terceiro_quartil=quantile(compSatisfaction, probs=.75),
                                 média=mean(compSatisfaction), mediana=median(compSatisfaction), primeiro_quartil=quantile(compSatisfaction, probs=.25), mínimo = min(compSatisfaction))
dataQuartiles.sat.melt.fd.05 <- melt(dataQuartiles.sat.fd.05, id.vars = "t")
dataQuartiles.sat.melt.fd.05$nof <- "FD"
dataQuartiles.sat.melt.fd.05$contenção <- 0.5

dataQuartiles.sat.sd.1 <- ddply(data[data$t%%60==0 & data$nof=="sd" & data$contention==1,], "t", summarise, máximo = max(compSatisfaction), terceiro_quartil=quantile(compSatisfaction, probs=.75),
                                média=mean(compSatisfaction), mediana=median(compSatisfaction), primeiro_quartil=quantile(compSatisfaction, probs=.25), mínimo = min(compSatisfaction))
dataQuartiles.sat.melt.sd.1 <- melt(dataQuartiles.sat.sd.1, id.vars = "t")
dataQuartiles.sat.melt.sd.1$nof <- "SD"
dataQuartiles.sat.melt.sd.1$contenção <- 1

dataQuartiles.sat.fd.1 <- ddply(data[data$t%%60==0 & data$nof=="fd" & data$contention==1,], "t", summarise, máximo = max(compSatisfaction), terceiro_quartil=quantile(compSatisfaction, probs=.75),
                                média=mean(compSatisfaction), mediana=median(compSatisfaction), primeiro_quartil=quantile(compSatisfaction, probs=.25), mínimo = min(compSatisfaction))
dataQuartiles.sat.melt.fd.1 <- melt(dataQuartiles.sat.fd.1, id.vars = "t")
dataQuartiles.sat.melt.fd.1$nof <- "FD"
dataQuartiles.sat.melt.fd.1$contenção <- 1

dataQuartiles.sat.melt = rbind(dataQuartiles.sat.melt.sd.05,dataQuartiles.sat.melt.fd.05,dataQuartiles.sat.melt.sd.1,dataQuartiles.sat.melt.fd.1)
# dataQuartiles.sat.melt = rbind(dataQuartiles.sat.melt.sd.05,dataQuartiles.sat.melt.fd.05)

dataQuartiles.sat.melt$nof = factor(dataQuartiles.sat.melt$nof, levels=c('SD','FD'))
dataQuartiles.sat.melt = dataQuartiles.sat.melt %>% mutate(cont_label = paste("E[k]:", contenção),
                                                             nof_label = paste("NoF:", nof))

dataQuartiles.sat.melt$nof_label = factor(dataQuartiles.sat.melt$nof_label, levels=c('NoF: SD','NoF: FD'))

dataQuartiles.sat.melt[dataQuartiles.sat.melt$t==86400 & dataQuartiles.sat.melt$nof=="SD" & dataQuartiles.sat.melt$contenção==0.5,]

library(ggplot2)
path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
png(paste(path$cycle,"satisfacao-lambda10-semcarona.png",sep=""), width=700, height=550)
ggplot(dataQuartiles.sat.melt[dataQuartiles.sat.melt$t<tempo_final,], aes(x = t, y = value, color = variable)) +
  theme_bw(base_size=15) + #scale_x_continuous(breaks = seq(0, tempo_final, by = 600)) +
  scale_y_continuous(breaks = seq(0,1, by = 0.25), limits = c(0,1)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) +
  facet_wrap(cont_label ~ nof_label, ncol=2) +
  #facet_grid(contenção ~ nof, labeller = label_both) + 
  ylab("satisfação") + theme(legend.title=element_blank()) + theme(legend.position = "right") +
  geom_line() 
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
dataQuartiles.fair.melt.sd.05$nof <- "SD"
dataQuartiles.fair.melt.sd.05$contenção <- 0.5

dataQuartiles.fair.fd.05 <- ddply(data[data$t%%60==0 & data$nof=="fd" & data$contention==0.5,], "t", summarise, máximo=max(fairness),terceiro_quartil=quantile(fairness, probs=.75),
                                 média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
dataQuartiles.fair.melt.fd.05 <- melt(dataQuartiles.fair.fd.05, id.vars = "t")
dataQuartiles.fair.melt.fd.05$nof <- "FD"
dataQuartiles.fair.melt.fd.05$contenção <- 0.5

dataQuartiles.fair.sd.1 <- ddply(data[data$t%%60==0 & data$nof=="sd" & data$contention==1,], "t", summarise, máximo=max(fairness),terceiro_quartil=quantile(fairness, probs=0.75),
                                média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
dataQuartiles.fair.melt.sd.1 <- melt(dataQuartiles.fair.sd.1, id.vars = "t")
dataQuartiles.fair.melt.sd.1$nof <- "SD"
dataQuartiles.fair.melt.sd.1$contenção <- 1

dataQuartiles.fair.fd.1 <- ddply(data[data$t%%60==0 & data$nof=="fd" & data$contention==1,], "t", summarise, máximo=max(fairness),terceiro_quartil=quantile(fairness, probs=.75),
                                média=mean(fairness), mediana=median(fairness), primeiro_quartil=quantile(fairness, probs=.25), mínimo = min(fairness))
dataQuartiles.fair.melt.fd.1 <- melt(dataQuartiles.fair.fd.1, id.vars = "t")
dataQuartiles.fair.melt.fd.1$nof <- "FD"
dataQuartiles.fair.melt.fd.1$contenção <- 1

dataQuartiles.fair.melt = rbind(dataQuartiles.fair.melt.sd.05,dataQuartiles.fair.melt.fd.05,dataQuartiles.fair.melt.sd.1,dataQuartiles.fair.melt.fd.1)

dataQuartiles.fair.melt$nof = factor(dataQuartiles.fair.melt$nof, levels=c('SD','FD'))

#colnames(dataQuartiles.fair.melt)[5] <- "contencao"

head(dataQuartiles.fair.melt)
dataQuartiles.fair.melt = dataQuartiles.fair.melt %>% mutate(cont_label = paste("E[k]:", contenção),
                                                             nof_label = paste("NoF:", nof))

dataQuartiles.fair.melt$nof_label = factor(dataQuartiles.fair.melt$nof_label, levels=c('NoF: SD','NoF: FD'))

dataQuartiles.fair.melt[dataQuartiles.fair.melt$t==86340 & dataQuartiles.fair.melt$nof=="FD" & dataQuartiles.fair.melt$contenção==1,]

library(ggplot2)
path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
png(paste(path$cycle,"fairness-lambda10-semcarona.png",sep=""), width=700, height=550)
ggplot(dataQuartiles.fair.melt[dataQuartiles.fair.melt$t<tempo_final,], aes(x = t, y = value, color = variable)) +
  theme_bw(base_size=15) + #scale_x_continuous(breaks = seq(0, tempo_final, by = 600)) +
  scale_y_continuous(breaks = seq(0,5, by = 1), limits = c(0,5)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) +
  facet_wrap(cont_label ~ nof_label, ncol=2) +
  #facet_grid(contenção ~ nof, labeller = label_both) + 
  ylab("paridade") + theme(legend.title=element_blank()) + theme(legend.position = "right") +
  geom_line()
dev.off()

dataQuartiles.fair.melt[dataQuartiles.fair.melt$t==84600,]




######################## satisfação sem ser calculada via complemento

data = rbind(data.semcaronas.kappa05,data.semcaronas.kappa1.sdnof,data.semcaronas.kappa1.fdnof)

dataQuartiles.sat.sd.05 <- ddply(data[data$t%%60==0 & data$nof=="sd" & data$contention==0.5,], "t", summarise, máximo = max(satisfaction), terceiro_quartil=quantile(satisfaction, probs=.75),
                                 média=mean(satisfaction), mediana=median(satisfaction), primeiro_quartil=quantile(satisfaction, probs=.25), mínimo = min(satisfaction))
dataQuartiles.sat.melt.sd.05 <- melt(dataQuartiles.sat.sd.05, id.vars = "t")
dataQuartiles.sat.melt.sd.05$nof <- "SD"
dataQuartiles.sat.melt.sd.05$contenção <- 0.5

dataQuartiles.sat.fd.05 <- ddply(data[data$t%%60==0 & data$nof=="fd" & data$contention==0.5,], "t", summarise, máximo = max(satisfaction), terceiro_quartil=quantile(satisfaction, probs=.75),
                                 média=mean(satisfaction), mediana=median(satisfaction), primeiro_quartil=quantile(satisfaction, probs=.25), mínimo = min(satisfaction))
dataQuartiles.sat.melt.fd.05 <- melt(dataQuartiles.sat.fd.05, id.vars = "t")
dataQuartiles.sat.melt.fd.05$nof <- "FD"
dataQuartiles.sat.melt.fd.05$contenção <- 0.5

dataQuartiles.sat.sd.1 <- ddply(data[data$t%%60==0 & data$nof=="sd" & data$contention==1,], "t", summarise, máximo = max(satisfaction), terceiro_quartil=quantile(satisfaction, probs=.75),
                                média=mean(satisfaction), mediana=median(satisfaction), primeiro_quartil=quantile(satisfaction, probs=.25), mínimo = min(satisfaction))
dataQuartiles.sat.melt.sd.1 <- melt(dataQuartiles.sat.sd.1, id.vars = "t")
dataQuartiles.sat.melt.sd.1$nof <- "SD"
dataQuartiles.sat.melt.sd.1$contenção <- 1

dataQuartiles.sat.fd.1 <- ddply(data[data$t%%60==0 & data$nof=="fd" & data$contention==1,], "t", summarise, máximo = max(satisfaction), terceiro_quartil=quantile(satisfaction, probs=.75),
                                média=mean(satisfaction), mediana=median(satisfaction), primeiro_quartil=quantile(satisfaction, probs=.25), mínimo = min(satisfaction))
dataQuartiles.sat.melt.fd.1 <- melt(dataQuartiles.sat.fd.1, id.vars = "t")
dataQuartiles.sat.melt.fd.1$nof <- "FD"
dataQuartiles.sat.melt.fd.1$contenção <- 1

dataQuartiles.sat.melt = rbind(dataQuartiles.sat.melt.sd.05,dataQuartiles.sat.melt.fd.05,dataQuartiles.sat.melt.sd.1,dataQuartiles.sat.melt.fd.1)
# dataQuartiles.sat.melt = rbind(dataQuartiles.sat.melt.sd.05,dataQuartiles.sat.melt.fd.05)

dataQuartiles.sat.melt$nof = factor(dataQuartiles.sat.melt$nof, levels=c('SD','FD'))
dataQuartiles.sat.melt = dataQuartiles.sat.melt %>% mutate(cont_label = paste("E[k]:", contenção),
                                                           nof_label = paste("NoF:", nof))

dataQuartiles.sat.melt$nof_label = factor(dataQuartiles.sat.melt$nof_label, levels=c('NoF: SD','NoF: FD'))

dataQuartiles.sat.melt[dataQuartiles.sat.melt$t==86400 & dataQuartiles.sat.melt$nof=="SD" & dataQuartiles.sat.melt$contenção==1,]
dataQuartiles.sat.melt[dataQuartiles.sat.melt$t==86400 & dataQuartiles.sat.melt$nof=="SD" & dataQuartiles.sat.melt$contenção==0.5,]


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


##########
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
  path$sdnof <- paste(path$sdnof,"1kappa-semCaronas/contention/",sep="")
  print(path$sdnof)
  data.sdnof <- load_data(path$sdnof)
  # data.sdnof$orderTime <- orderTime
  # data.sdnof$kappa <- movingAverage(data.sdnof$kappa,ma)
  
  # data <- rbind(data.sdnof, data.fdnof)
  
  data.sdnof
}

experiment <- paste(cycle,"minutes",sep="")
data.cycle10.sdnof.contention = get_contention(orderTime,experiment, cycle, ma=1)

data.cycle10.sdnof.contention[data.cycle10.sdnof.contention$t==57600,]$kappa
