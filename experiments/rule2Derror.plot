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
set output 'rule2DerrorCmax.pdf'
plot  "data/cmax.csv" using 1:4:6 with filledcurves lc "light-blue" notitle,\
      "data/cmax.csv" using 1:5 with lines lc "dark-blue" title 'Gift',\
      "data/cmax.csv" using 1:19:21 with filledcurves lc "light-red" notitle,\
      "data/cmax.csv" using 1:20 with lines lc "dark-red" title 'LP'
set ylabel "Flowtime"
set output 'rule2DerrorFlowtime.pdf'
set xlabel "m workers (10m tasks)"
plot  "data/flowtime.csv" using 1:4:6 with filledcurves lc "light-blue" notitle,\
       "data/flowtime.csv" using 1:5 with lines lc "dark-blue" title 'Gift',\
       "data/flowtime.csv" using 1:19:21 with filledcurves lc "light-red" notitle,\
       "data/flowtime.csv" using 1:20 with lines lc "dark-red" title 'LP'