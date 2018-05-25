set terminal pdf
set datafile separator ","
set style fill solid border rgb "black"
set auto x
set auto y
set grid
set ticslevel 0
set style data lines
set xlabel "m workers (2m tasks)"
set ylabel "Cmax"
set output 'giftVsExhau2DCmax.pdf'
plot  "data/mincmax.csv" using 1:4:6 with filledcurves lc "light-green" notitle,\
      "data/mincmax.csv" using 1:5 with lines lc "dark-green" title 'min Cmax',\
      "data/mincmax.csv" using 1:9:11 with filledcurves lc "light-blue" notitle,\
      "data/mincmax.csv" using 1:10 with lines lc "dark-blue" title 'Gift',\
      "data/mincmax.csv" using 1:14:16 with filledcurves lc "light-red" notitle,\
      "data/mincmax.csv" using 1:15 with lines lc "dark-red" title 'Gift'