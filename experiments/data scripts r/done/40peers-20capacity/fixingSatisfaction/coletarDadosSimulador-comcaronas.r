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
write.csv(data.comcaronas.kappa05.sdnof, file = paste(paste(path$orderTime,"resultados-sdnof-kappa05-comcaronas.csv",sep="")))

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