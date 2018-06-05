set terminal pdf
set datafile separator ","
set style fill solid border rgb "black"
set style fill transparent solid 0.5 noborder
set auto x
set auto y
set grid
set ticslevel 0
set style data lines
set xlabel "m workers (5m tasks)"
set ylabel "Makespan"
set key center right
set output 'figures/rule2DerrorCmax.pdf'
plot  "data/cmax.csv" using 1:4:6 with filledcurves lc "light-blue" notitle,\
      "data/cmax.csv" using 1:5 with lines dt 3 lc "dark-blue" title '(Dis)Gift',\
      "data/cmax.csv" using 1:14:16 with filledcurves lc "light-magenta" notitle,\
      "data/cmax.csv" using 1:15 with lines dt 7 lc "dark-magenta" title '(Dis)Swap',\
      "data/cmax.csv" using 1:19:21 with filledcurves lc "light-red" notitle,\
      "data/cmax.csv" using 1:20 with lines dt 5 lc "dark-red" title 'LP'
set ylabel "Flowtime"
set xlabel "m workers (10m tasks)"
set output 'figures/rule2DerrorFlowtime.pdf'
plot  "data/flowtime.csv" using 1:4:6 with filledcurves lc "light-blue" notitle,\
      "data/flowtime.csv" using 1:5 with lines dt 3 lc "dark-blue" title '(Dis)Gift',\
      "data/flowtime.csv" using 1:14:16 with filledcurves lc "light-magenta" notitle,\
       "data/flowtime.csv" using 1:15 with lines dt 7 lc "dark-magenta" title '(Dis)Swap',\
      "data/flowtime.csv" using 1:19:21 with filledcurves lc "light-red" notitle,\
      "data/flowtime.csv" using 1:20 with lines dt 5 lc "dark-red" title 'LP'
