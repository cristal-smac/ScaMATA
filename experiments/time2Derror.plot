set terminal pdfcairo
set datafile separator ","
set style fill solid border rgb "black"
set style fill transparent solid 0.5 noborder
set auto x
set auto y
set grid
set ticslevel 0
set key font ",20"
set key left above
set style data lines
set xlabel "m workers (5m tasks)"
set ylabel "Time (s)"
#set log y
set key bottom right
set output 'figures/time2DerrorCmax.pdf'
plot "data/cmax.csv" using 1:($29)/1E9:($31)/1E9 with filledcurves lc "light-blue" notitle,\
     "data/cmax.csv" using 1:($30)/1E9 with lines dt 3 lc "dark-blue" title 'Gift',\
     "data/cmax.csv" using 1:($34)/1E9:($36)/1E9 with filledcurves lc "light-green" notitle,\
     "data/cmax.csv" using 1:($35)/1E9 with lines dt 1 lc "dark-green" title 'Dis. Gift',\
     "data/cmax.csv" using 1:($39)/1E9:($41)/1E9 with filledcurves lc "light-magenta" notitle,\
     "data/cmax.csv" using 1:($40)/1E9 with lines dt 7 lc "dark-magenta" title 'Swap',\
     "data/cmax.csv" using 1:($44)/1E9:($46)/1E9 with filledcurves lc "light-cyan" notitle,\
     "data/cmax.csv" using 1:($45)/1E9 with lines dt 9 lc "dark-cyan" title 'Dis. Swap',\
     "data/cmax.csv" using 1:($49)/1E9:($51)/1E9 with filledcurves lc "light-red" notitle,\
     "data/cmax.csv" using 1:($50)/1E9 with lines dt 5 lc "dark-red" title 'ECT'
set output 'figures/time2DerrorFlowtime.pdf'
#set log y
set xlabel "m workers (5m tasks)"
plot "data/flowtime.csv" using 1:($29)/1E9:($31)/1E9 with filledcurves lc "light-blue" notitle,\
     "data/flowtime.csv" using 1:($30)/1E9 with lines dt 3 lc "dark-blue" title 'Gift (LF)',\
     "data/flowtime.csv" using 1:($34)/1E9:($36)/1E9 with filledcurves lc "light-green" notitle,\
     "data/flowtime.csv" using 1:($35)/1E9 with lines dt 1 lc "dark-green" title 'Dis. Gift (LC)',\
     "data/flowtime.csv" using 1:($39)/1E9:($41)/1E9 with filledcurves lc "light-magenta" notitle,\
     "data/flowtime.csv" using 1:($40)/1E9 with lines dt 7 lc "dark-magenta" title 'Swap (LF)',\
     "data/flowtime.csv" using 1:($44)/1E9:($46)/1E9 with filledcurves lc "light-cyan" notitle,\
     "data/flowtime.csv" using 1:($45)/1E9 with lines dt 9 lc "dark-cyan" title 'Dis. Swap (LC)',\
     "data/flowtime.csv" using 1:($49)/1E9:($51)/1E9 with filledcurves lc "light-red" notitle,\
     "data/flowtime.csv" using 1:($50)/1E9 with lines dt 5 lc "dark-red" title 'Min. flowtime',\
     "data/flowtime.csv" using 1:(25*($1)**3)/1E6 with lines dt 11 lc "black" title 'Theo'