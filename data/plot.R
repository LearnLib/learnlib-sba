library("ggplot2")
library("grid")
library("gridExtra")

options(scipen=999)

myPlot <- function(data, sddata, column, title) {
  gg = ggplot(data, aes(x = Size, y = get(column), color=Type, shape=Type))
  gg = gg + geom_point(size=2)
  gg = gg + geom_line()
  gg = gg + geom_errorbar(aes(ymin=.data[[column]]-sddata[[column]], ymax=.data[[column]]+sddata[[column]], color=Type), width=3)
  gg = gg + labs(x = "Procedural size", y = column)
  gg = gg + scale_x_continuous(breaks=seq(0, 100, 10))
  gg = gg + theme_minimal()
  return(gg)
}

sbaCSV = read.csv("./random-sba.csv", strip.white = TRUE)

sbaMean = aggregate(. ~ Type + Name + Size + Opt, sbaCSV, mean)
sbaSD = aggregate(. ~ Type + Name + Size + Opt, sbaCSV, sd)

completeMean = sbaMean[sbaMean$Name == "complete" & sbaMean$Opt == "false",]
completeSD = sbaSD[sbaSD$Name == "complete" & sbaMean$Opt == "false",]
partialMean = sbaMean[sbaMean$Name == "partial" & sbaMean$Opt == "false",]
partialSD = sbaSD[sbaSD$Name == "partial" & sbaMean$Opt == "false",]


# plot(myPlot(completeMean, completeSD, "Queries", "Complete System"))
# plot(myPlot(partialMean, partialSD, "Queries", "Partial System"))

# gg = ggplot(sbaMean[sbaMean$Name == "complete",], aes(x = Size, y = Queries, shape=Type))
# gg = gg + geom_point(size=2)
# gg = gg + geom_line()
# gg = gg + geom_errorbar(aes(ymin=Queries-sbaSD$Queries, ymax=Queries+sbaSD$Queries), width=.2)
# gg = gg + coord_cartesian(ylim = c(1e+05, 1e+10))
# gg = gg + labs(x = "Size", y = "Symbols [#]", title = "SPMM")
# gg = gg + theme(legend.position = "none")
# plot(gg)

myWidth = 5
myHeight = 1.5

pdf("./complete-queries.pdf", width=myWidth, height=myHeight)
plot(myPlot(completeMean, completeSD, "Queries", "Complete System"))
dev.off()

pdf("./complete-symbols.pdf", width=myWidth, height=myHeight)
plot(myPlot(completeMean, completeSD, "Symbols", "Complete System"))
dev.off()

pdf("./partial-queries.pdf", width=myWidth, height=myHeight)
plot(myPlot(partialMean, partialSD, "Queries", "Partial System"))
dev.off()

pdf("./partial-symbols.pdf", width=myWidth, height=myHeight)
plot(myPlot(partialMean, partialSD, "Symbols", "Partial System"))
dev.off()


pdf("./bundle.pdf", width=20, height=10)
gg1 = myPlot(completeMean, completeSD, "Queries", "Complete System") + labs(title="Complete System")
gg2 = myPlot(completeMean, completeSD, "Symbols", "Complete System") + labs(title="Complete System")
gg3 = myPlot(partialMean, partialSD, "Queries", "Partial System") + labs(title="Complete System")
gg4 = myPlot(partialMean, partialSD, "Symbols", "Partial System") + labs(title="Complete System")

grid.arrange(gg1, gg2, gg3, gg4, ncol=2)
dev.off()

