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
      if(x[j,]$sFed>0){
        prov <- prov + (x[j,]$sFed * (x[j,]$t - x[j-1,]$t))
      }
      if(x[j,]$rFed>0){
        cons <- cons + (x[j,]$rFed * (x[j,]$t - x[j-1,]$t))
      }
      if(x[j,]$dFed>0){
        req <- req + (x[j,]$dFed * (x[j,]$t - x[j-1,]$t))
      }
      if(x[j,]$oFed>0){
        offered <- offered + (x[j,]$oFed * (x[j,]$t - x[j-1,]$t))
      }
      
      x[j,]$prov <- prov
      x[j,]$cons <- cons
      x[j,]$req <- req
      x[j,]$offered <- offered
      
      if(x[j,]$prov==0){
        x[j,]$fairness <- -1
      } else {
        x[j,]$fairness <- cons/prov
      }
      
      
      if(x[j,]$t>=tempo_final && finished == FALSE){
        x[j,]$t <- tempo_final
        finished <- TRUE
      }else if(x[j,]$t>=tempo_final && finished == TRUE){
        x[j,]$t <- -1
      }
      
      if(x[j,]$req==0){
        x[j,]$satisfaction <- -1
      } else{
        x[j,]$satisfaction <- cons/req
      }
    }
    x
  }
  result<-result[!(result$t==-1),]
  return(result)
}

#path <- "~/Área de Trabalho/Experimentos-Doutorado/scripts r/done/"
#path$exp <- paste(path,"40peers-20capacity/randomNof/cycle",sep="")
path <- "/home/eduardolfalcao/workspace3/fogbow-manager/experiments/data scripts r/done/"
path$exp <- paste(path,"40peers-20capacity/prioritizationFixed/cycle",sep="")


adjust_data <- function(tempo_final, cycle){
  
  path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
  
  path$sdnof.weightedSelection <- paste(path$cycle,"sdnof-weightedSelection/",sep="")
  data.sdnof.weightedSelection <- load_data(path$sdnof.weightedSelection)
  data.sdnof.weightedSelection <- create_columns(data.sdnof.weightedSelection, FALSE, 20, cycle)
  data.sdnof.weightedSelection <- compute_results(data.sdnof.weightedSelection, tempo_final)
  data.sdnof.weightedSelection$selection <- "weighted"
  
  path$sdnof.weightedBroadcastSelection <- paste(path$cycle,"sdnof-weightedBroadcastSelection/",sep="")
  data.sdnof.weightedBroadcastSelection <- load_data(path$sdnof.weightedBroadcastSelection)
  data.sdnof.weightedBroadcastSelection <- create_columns(data.sdnof.weightedBroadcastSelection, FALSE, 20, cycle)
  data.sdnof.weightedBroadcastSelection <- compute_results(data.sdnof.weightedBroadcastSelection, tempo_final)
  data.sdnof.weightedBroadcastSelection$selection <- "weighted broadcast"
  
  #path$fdnof <- paste(path$cycle,"fdnof/",sep="")
  #data.fdnof <- load_data(path$fdnof)
  #data.fdnof <- create_columns(data.fdnof, TRUE, 20, cycle)
  #data.fdnof <- compute_results(data.fdnof, tempo_final)
  
  #data <- rbind(data.sdnof, data.fdnof)
  data <- rbind(data.sdnof.weightedSelection, data.sdnof.weightedBroadcastSelection)  
  
  data
}

get_contention <- function(cycle){
  path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")
  
  path$sdnof.weightedSelection <- paste(path$cycle,"sdnof-weightedSelection/",sep="")
  path$contention <- paste(path$sdnof.weightedSelection,"/contention/",sep="")
  data.sdnof.weightedSelection <- load_data(path$contention)
  
  path$sdnof.weightedBroadcastSelection <- paste(path$cycle,"sdnof-weightedBroadcastSelection/",sep="")
  path$contention <- paste(path$sdnof.weightedBroadcastSelection,"/contention/",sep="")
  data.sdnof.weightedBroadcastSelection <- load_data(path$contention)
  
  #path$nof <- paste(path$cycle,"fdnof",sep="")
  #path$contention <- paste(path$nof,"/contention/",sep="")
  #data.fdnof <- load_data(path$contention)
  #data <- rbind(data.fdnof, data.sdnof)
  
  data <- rbind(data.sdnof.weightedSelection, data.sdnof.weightedBroadcastSelection)
  
  data
}
  

tempo_final = 42000

cycle = 10
data.10cycle = adjust_data(tempo_final, cycle)
data.10cycle.contention = get_contention(cycle)

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

#############################

#install.packages("ggplot2")
library(ggplot2)


cycle <- 10
data <- data.10cycle
path$cycle <- paste(paste(path$exp,cycle,sep=""),"/",sep="")

png(paste(path$cycle,"fairness.png",sep=""), width=1280, height=720)
ggplot(data[data$cycle==cycle,], aes(t, fairness)) + 
  geom_line(aes(colour=selection, group=interaction(selection,id))) +
  theme_bw() + theme(legend.position = "top") + ylim(0,5) + scale_x_continuous(breaks = seq(0, tempo_final, by = 3600)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1))
dev.off()

png(paste(path$cycle,"satisfaction.png",sep=""), width=1280, height=720)
ggplot(data[data$cycle==cycle,], aes(t, satisfaction)) + 
  geom_line(aes(colour=selection, group=interaction(selection,id))) +
  theme_bw() + theme(legend.position = "top") + scale_x_continuous(breaks = seq(0, tempo_final, by = 3600)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) + ylim(0,1)
dev.off()

png(paste(path$cycle,"contention.png",sep=""), width=1280, height=720)
ggplot(data.10cycle.contention, aes(t, kappa, colour=nof)) + geom_line() + theme_bw() + theme(legend.position = "top") + scale_x_continuous(breaks = seq(0, tempo_final, by = 600)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) #+ ylim(0,50)
dev.off()

png(paste(path$cycle,"contention.png",sep=""), width=1280, height=720)
ggplot(data.30cycle.contention, aes(t, kappa, colour=nof)) + geom_line() + theme_bw() + theme(legend.position = "top") + scale_x_continuous(breaks = seq(0, tempo_final, by = 1800)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1))
dev.off()

png(paste(path$cycle,"contention.png",sep=""), width=1280, height=720)
ggplot(data.60cycle.contention, aes(t, kappa, colour=nof)) + geom_line() + theme_bw() + theme(legend.position = "top") + scale_x_continuous(breaks = seq(0, tempo_final, by = 3600)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) + ylim(0,5)
dev.off()





summary(data[data$cycle==60 & data$nof=="fd" & data$t==42000,])

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

