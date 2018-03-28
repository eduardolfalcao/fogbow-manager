#tabela final

simulador.sdnof.05.semcaronas = read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/simulator-validation/sdnof-semcaronasPI0.5kappa05FRsemcaronaslMin0.75delta0.05.csv", header=TRUE, sep=",")

sum(simulador.sdnof.05.semcaronas$satisfaction)/40
sum(simulador.sdnof.05.semcaronas$fairness)/40

simulador.fdnof.05.semcaronas = read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/simulator-validation/fdnof-semcaronasPI0.5kappa05FRsemcaronaslMin0.75delta0.05.csv", header=TRUE, sep=",")

sum(simulador.fdnof.05.semcaronas$satisfaction)/40
sum(simulador.fdnof.05.semcaronas$fairness)/40

simulador.sdnof.05.comcaronas = read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/simulator-validation/sdnof-comcaronas-15-02PI0.7kappa05FRcomcaronaslMin0.75delta0.05.csv", header=TRUE, sep=",")
#simulador.sdnof.05.comcaronas = read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/simulator-validation/sdnof-comcaronasPI0.7kappa05FRcomcaronaslMin0.75delta0.05.csv", header=TRUE, sep=",")
sum(simulador.sdnof.05.comcaronas[1:40,]$satisfaction)/40
sum(simulador.sdnof.05.comcaronas[1:40,]$fairness)/40

simulador.fdnof.05.comcaronas = read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/simulator-validation/fdnof-comcaronas-15-02PI0.5kappa05FRcomcaronaslMin0.75delta0.05.csv", header=TRUE, sep=",")
#simulador.fdnof.05.comcaronas = read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/simulator-validation/fdnof-comcaronasPI0.5kappa05FRcomcaronaslMin0.75delta0.05.csv", header=TRUE, sep=",")
sum(simulador.fdnof.05.comcaronas[1:40,]$satisfaction)/40
sum(simulador.fdnof.05.comcaronas[1:40,]$fairness)/40

sum(k05comcaronas.sdnof[k05comcaronas.sdnof$t==86340 & k05comcaronas.sdnof$id<=40,]$compSatisfaction)/40
sum(k05comcaronas.sdnof[k05comcaronas.sdnof$t==86340 & k05comcaronas.sdnof$id<=40,]$fairness)/40

sum(k05comcaronas.fdnof[k05comcaronas.fdnof$t==86340 & k05comcaronas.fdnof$id<=40,]$compSatisfaction)/40
sum(k05comcaronas.fdnof[k05comcaronas.fdnof$t==86340 & k05comcaronas.fdnof$id<=40,]$fairness)/40

k05semcaronas <- read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/resultados-kappa05-semcaronas.csv", header=TRUE, sep=",")
library(stringi)
k05semcaronas$id <- stri_replace_all_regex(k05semcaronas$id, "p", "")
k05semcaronas$id <- sapply( k05semcaronas$id, as.numeric )

sum(k05semcaronas[k05semcaronas$t==86340 & k05semcaronas$id<=40 & k05semcaronas$nof=="sd",]$compSatisfaction)/40
sum(k05semcaronas[k05semcaronas$t==86340 & k05semcaronas$id<=40 & k05semcaronas$nof=="sd",]$fairness)/40

k05semcaronas[k05semcaronas$t==86340 & k05semcaronas$id<=40 & k05semcaronas$nof=="fd",]

sum(k05semcaronas[k05semcaronas$t==86340 & k05semcaronas$id<=40 & k05semcaronas$nof=="fd",]$compSatisfaction)/40
sum(k05semcaronas[k05semcaronas$t==86340 & k05semcaronas$id<=40 & k05semcaronas$nof=="fd",]$fairness)/40



#sem caronas
k05semcaronas <- read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/resultados-kappa05-semcaronas.csv", header=TRUE, sep=",")

sum(k05semcaronas[k05semcaronas$t==86340 & k05semcaronas$nof=="sd",]$tDFed)/(86340*40)
sum(k05semcaronas[k05semcaronas$t==86340 & k05semcaronas$nof=="fd",]$tDFed)/(86340*40)

sum(k05semcaronas[k05semcaronas$t==86340 & k05semcaronas$nof=="sd",]$dTotAcumulado)/sum(k05semcaronas[k05semcaronas$t==86340 & k05semcaronas$nof=="sd",]$tDTot)
sum(k05semcaronas[k05semcaronas$t==86340 & k05semcaronas$nof=="fd",]$dTotAcumulado)/sum(k05semcaronas[k05semcaronas$t==86340 & k05semcaronas$nof=="fd",]$tDTot)

data.semcaronas.kappa05 <- read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/resultados-kappa05-semcaronas07-02-2018.csv", header=TRUE, sep=",")
data.semcaronas.kappa05$id <- stri_replace_all_regex(data.semcaronas.kappa05$id, "p", "")
data.semcaronas.kappa05$id <- sapply( data.semcaronas.kappa05$id, as.numeric )

sum(data.semcaronas.kappa05[data.semcaronas.kappa05$t==86340 & data.semcaronas.kappa05$nof=="sd",]$dFedAcumulado)/sum(data.semcaronas.kappa05[data.semcaronas.kappa05$t==86340 & data.semcaronas.kappa05$nof=="sd",]$tDFed)
sum(data.semcaronas.kappa05[data.semcaronas.kappa05$t==86340 & data.semcaronas.kappa05$nof=="fd",]$dFedAcumulado)/sum(data.semcaronas.kappa05[data.semcaronas.kappa05$t==86340 & data.semcaronas.kappa05$nof=="fd",]$tDFed)

data.comcaronas.kappa05
data.comcaronas.kappa05$id <- stri_replace_all_regex(data.comcaronas.kappa05$id, "p", "")
data.comcaronas.kappa05$id <- sapply( data.comcaronas.kappa05$id, as.numeric )

sum(data.comcaronas.kappa05[data.comcaronas.kappa05$t==86340 & data.comcaronas.kappa05$nof=="sd",]$dFedAcumulado)/sum(data.comcaronas.kappa05[data.comcaronas.kappa05$t==86340 & data.comcaronas.kappa05$nof=="sd",]$tDFed)
sum(data.comcaronas.kappa05[data.comcaronas.kappa05$t==86340 & data.comcaronas.kappa05$nof=="fd",]$dFedAcumulado)/sum(data.comcaronas.kappa05[data.comcaronas.kappa05$t==86340 & data.comcaronas.kappa05$nof=="fd",]$tDFed)

k05semcaronas[k05semcaronas$t==86340 & k05semcaronas$nof=="fd",]
sum(k05semcaronas[k05semcaronas$t==86340 & k05semcaronas$id<=40,]$dTotAcumulado)/sum(k05comcaronas.fdnof[k05comcaronas.fdnof$t==86340 & k05comcaronas.fdnof$id<=40,]$tDTot)

k1semcaronas.sdnof <- read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/resultados-sdnof-kappa1-semcaronas.csv", header=TRUE, sep=",")
sum(k1semcaronas.sdnof[k1semcaronas.sdnof$t==86340,]$tDFed)/(86340*40)
sum(k1semcaronas.sdnof[k1semcaronas.sdnof$t==86340,]$dTotAcumulado)/sum(k1semcaronas.sdnof[k1semcaronas.sdnof$t==86340,]$tDTot)

k1semcaronas.fdnof <- read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/resultados-fdnof-kappa1-semcaronas.csv", header=TRUE, sep=",")
sum(k1semcaronas.fdnof[k1semcaronas.fdnof$t==86340,]$tDFed)/(86340*40)
sum(k1semcaronas.fdnof[k1semcaronas.fdnof$t==86340,]$dTotAcumulado)/sum(k1semcaronas.fdnof[k1semcaronas.fdnof$t==86340,]$tDTot)




nrow(k1semcaronas.sdnof[k1semcaronas.sdnof$t==86400,])



k1semcaronas.fdnof[k1semcaronas.fdnof$id=="p20" & k1semcaronas.fdnof$t==60000,]$fairness
summary(k1semcaronas.fdnof[k1semcaronas.fdnof$t==60000,]$fairness)

quantile(k1semcaronas.fdnof$fairness, probs=.9)


k1semcaronas.fdnof[k1semcaronas.fdnof$id=="p20" & k1semcaronas.fdnof$t>60000,]$compSatisfaction


#com dFed
k05comcaronas <- read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/resultados-kappa05-comcaronas09-02-2018.csv", header=TRUE, sep=",")
sum(k05comcaronas[k05comcaronas$t==86340 & k05comcaronas$nof=="sd",]$dTotAcumulado)/sum(k05comcaronas[k05comcaronas$t==86340 & k05comcaronas$nof=="sd",]$tDTot)
sum(k05comcaronas[k05comcaronas$t==86340 & k05comcaronas$nof=="sd",]$dFedAcumulado)/sum(k05comcaronas[k05comcaronas$t==86340 & k05comcaronas$nof=="sd",]$tDFed)
sum(k05comcaronas[k05comcaronas$t==86340 & k05comcaronas$nof=="fd",]$dTotAcumulado)/sum(k05comcaronas[k05comcaronas$t==86340 & k05comcaronas$nof=="fd",]$tDTot)
sum(k05comcaronas[k05comcaronas$t==86340 & k05comcaronas$nof=="fd",]$dFedAcumulado)/sum(k05comcaronas[k05comcaronas$t==86340 & k05comcaronas$nof=="fd",]$tDFed)

k05comcaronas.sdnof <- read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/resultados-sdnof-kappa05-comcaronas.csv", header=TRUE, sep=",")
library(stringi)
k05comcaronas.sdnof$id <- stri_replace_all_regex(k05comcaronas.sdnof$id, "p", "")
k05comcaronas.sdnof$id <- sapply( k05comcaronas.sdnof$id, as.numeric )
sum(k05comcaronas.sdnof[k05comcaronas.sdnof$t==86340 & k05comcaronas.sdnof$id<=40,]$tDFed)/(86340*40)
sum(k05comcaronas.sdnof[k05comcaronas.sdnof$t==86340 & k05comcaronas.sdnof$id<=40,]$dTotAcumulado)/sum(k05comcaronas.sdnof[k05comcaronas.sdnof$t==86340 & k05comcaronas.sdnof$id<=40,]$tDTot)
sum(k05comcaronas.sdnof[k05comcaronas.sdnof$t==86340 & k05comcaronas.sdnof$id<=40,]$dFedAcumulado)/sum(k05comcaronas.sdnof[k05comcaronas.sdnof$t==86340 & k05comcaronas.sdnof$id<=40,]$tDFed)


k05comcaronas.fdnof <- read.csv(file="/home/eduardolfalcao/git/fogbow-manager/experiments/data scripts r/done/40peers-20capacity/fixingSatisfaction/cycle10/resultados-fdnof-kappa05-comcaronas.csv", header=TRUE, sep=",")
k05comcaronas.fdnof$id <- stri_replace_all_regex(k05comcaronas.fdnof$id, "p", "")
k05comcaronas.fdnof$id <- sapply( k05comcaronas.fdnof$id, as.numeric )
sum(k05comcaronas.fdnof[k05comcaronas.fdnof$t==86340 & k05comcaronas.fdnof$id<=40,]$fairness)/40
sum(k05comcaronas.fdnof[k05comcaronas.fdnof$t==86340 & k05comcaronas.fdnof$id<=40,]$compSatisfaction)/40

k05comcaronas.fdnof[k05comcaronas.fdnof$t==86340 & k05comcaronas.fdnof$id<=40,]


sum(k05comcaronas.fdnof[k05comcaronas.fdnof$t==86340 & k05comcaronas.fdnof$id<=40,]$tDFed)/(86340*40)
sum(k05comcaronas.fdnof[k05comcaronas.fdnof$t==86340 & k05comcaronas.fdnof$id<=40,]$dTotAcumulado)/sum(k05comcaronas.fdnof[k05comcaronas.fdnof$t==86340 & k05comcaronas.fdnof$id<=40,]$tDTot)


sum(k05comcaronas.sdnof[k05comcaronas.sdnof$t==86340 & k05comcaronas.sdnof$id>40,]$tDFed)/(86340*10)
sum(k05comcaronas.sdnof[k05comcaronas.sdnof$t==86340 & k05comcaronas.sdnof$id>40,]$tDFed)/(86340*10)

k05comcaronas.sdnof[k05comcaronas.sdnof$t==86340 & k05comcaronas.sdnof$id>40,]
k05comcaronas.sdnof[k05comcaronas.sdnof$id==45 & k05comcaronas.sdnof$t>0,]

k05comcaronas.fdnof[k05comcaronas.fdnof$t==86340 & k05comcaronas.fdnof$id>40,]







# install.packages("stringi")




summary(k05comcaronas.sdnof[k05comcaronas.sdnof$t==86400 & k05comcaronas.sdnof$id>40,]$compSatisfaction)

k05comcaronas.fdnof[k05comcaronas.fdnof$t==86400 & k05comcaronas.fdnof$compSatisfaction<0.5 & k05comcaronas.fdnof$id==16,]


#k05comcaronas.fdnof[k05comcaronas.fdnof$t==86400 & k05comcaronas.fdnof$id==20,]

#6,16,20
library(ggplot2)
ggplot(k05comcaronas.fdnof[k05comcaronas.fdnof$t<=86400 & (k05comcaronas.fdnof$id==6 | k05comcaronas.fdnof$id==16 | k05comcaronas.fdnof$id==20),], aes(t, fairness)) + 
  geom_line(aes(colour=id, group=factor(interaction(nof,id)))) +
  theme_bw(base_size=15) + theme(legend.position = "right") + #scale_x_continuous(breaks = seq(0, tempo_final, by = 1200)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1), plot.title = element_text(hjust = 0.5)) +
  ylab("justiça") + ylim(0,2) + theme(legend.position = "top") 
#ggtitle("order time = 6.6min, lambda = 10, 50 peers")
dev.off()



ggplot(k05comcaronas.fdnof[k05comcaronas.fdnof$t<=86400 & (k05comcaronas.fdnof$id<=40),], aes(t, fairness)) + 
  geom_line(aes(colour=id, group=factor(interaction(nof,id)))) +
  theme_bw(base_size=15) + theme(legend.position = "right") + #scale_x_continuous(breaks = seq(0, tempo_final, by = 1200)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1), plot.title = element_text(hjust = 0.5)) +
  ylab("justiça") + ylim(0,7) + theme(legend.position = "top") 



summary(k05comcaronas.fdnof[k05comcaronas.fdnof$t==86400 & k05comcaronas.fdnof$id>40,]$compSatisfaction)

