set terminal pdf
set termoption dashed
set datafile separator ","
set style fill solid border rgb "black"
set style fill transparent solid 0.7 noborder
set auto x
set auto y
set grid
set ticslevel 0
set style data lines
set key center right
set xlabel "m workers (2m tasks)"
set ylabel "Makespan"
set output 'figures/giftVsExhau2DCmax.pdf'
plot  "data/mincmax.csv" using 1:4:6 with filledcurves lc "light-green" notitle,\
      "data/mincmax.csv" using 1:5 with lines dt 1 lc "dark-green" title 'min makespan',\
      "data/mincmax.csv" using 1:9:11 with filledcurves lc "light-blue" notitle,\
      "data/mincmax.csv" using 1:10 with lines dt 3 lc "dark-blue" title '(Dis)Gift',\
      "data/mincmax.csv" using 1:14:16 with filledcurves lc "light-cyan" notitle,\
      "data/mincmax.csv" using 1:15 with lines dt 3 lc "dark-cyan" title '(Dis)Gift',\
      "data/mincmax.csv" using 1:19:21 with filledcurves lc "light-red" notitle,\
      "data/mincmax.csv" using 1:20 with lines dt 5 lc "dark-red" title 'LP'