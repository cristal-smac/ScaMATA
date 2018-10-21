set terminal pdfcairo
set datafile separator ","
set style fill solid border rgb "black"
set style fill transparent solid 0.1 noborder
set auto x
set auto y
set grid
set ticslevel 0
set style data lines
set xlabel "m workers (5m tasks)"
set ylabel "Makespan"
# set log y
set key center right
set output 'figures/rule2DerrorCmax.pdf'
plot  "data/cmax.csv" using 1:4:6 with filledcurves lc "light-blue" notitle,\
      "data/cmax.csv" using 1:5 with lines dt 3 lc "dark-blue" title 'Gift',\
      "data/cmax.csv" using 1:14:16 with filledcurves lc "light-magenta" notitle,\
      "data/cmax.csv" using 1:15 with lines dt 7 lc "dark-magenta" title 'Swap',\
      "data/cmax.csv" using 1:24:26 with filledcurves lc "light-red" notitle,\
      "data/cmax.csv" using 1:25 with lines dt 5 lc "dark-red" title 'LP'
#      "data/cmax.csv" using 1:19:21 with filledcurves lc "cyan" notitle,\
#      "data/cmax.csv" using 1:20 with lines dt 7 lc "dark-cyan" title 'DisSwap',\
#      "data/cmax.csv" using 1:9:11 with filledcurves lc "light-green" notitle,\
#      "data/cmax.csv" using 1:10 with lines dt 3 lc "dark-green" title 'DisGift',\
set ylabel "Flowtime"
set xlabel "m workers (5m tasks)"
set output 'figures/rule2DerrorFlowtime.pdf'
plot  "data/flowtime.csv" using 1:4:6 with filledcurves lc "light-blue" notitle,\
      "data/flowtime.csv" using 1:5 with lines dt 3 lc "dark-blue" title 'Gift',\
      "data/flowtime.csv" using 1:14:16 with filledcurves lc "light-magenta" notitle,\
      "data/flowtime.csv" using 1:15 with lines dt 7 lc "dark-magenta" title 'Swap',\
      "data/flowtime.csv" using 1:24:26 with filledcurves lc "light-red" notitle,\
      "data/flowtime.csv" using 1:25 with lines dt 5 lc "dark-red" title 'LP'
#      "data/flowtime.csv" using 1:9:11 with filledcurves lc "light-green" notitle,\
#      "data/flowtime.csv" using 1:10 with lines dt 3 lc "dark-green" title 'DisGift',\
#      "data/flowtime.csv" using 1:19:21 with filledcurves lc "cyan" notitle,\
#      "data/flowtime.csv" using 1:20 with lines dt 7 lc "dark-cyan" title 'DisSwap',\
