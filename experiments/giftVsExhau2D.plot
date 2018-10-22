set terminal pdfcairo
set termoption dashed
set datafile separator ","
set style fill solid border rgb "black"
set style fill transparent solid 0.3 noborder
set auto x
set auto y
set grid
set ticslevel 0
set style data lines
set key left above
set xtics 1
set xlabel "m workers (10 tasks)"
set ylabel "Makespan"
set output 'figures/giftVsExhau2DCmax.pdf'
plot  "data/mincmax.csv" using 1:5 with lines dt 1 lc "dark-green" title 'Minimal makespan',\
      "data/mincmax.csv" using 1:10 with lines dt 3 lc "dark-blue" title '(Dis)Gift',\
      "data/mincmax.csv" using 1:15 with lines dt 7 lc "dark-magenta" title '(Dis)Swap',\
      "data/mincmax.csv" using 1:20 with lines dt 5 lc "dark-red" title 'ECT'