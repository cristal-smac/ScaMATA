set terminal pdf
set datafile separator ","
set style fill solid border rgb "black"
set auto x
set auto y
set grid
set ticslevel 0
set style data lines
set xlabel "m workers (10m tasks)"
set ylabel "Time (s)"
set output 'time2DerrorCmax.pdf'
plot "data/cmax.csv" using 1:($24)/1E9:($26)/1E9 with filledcurves lc "light-blue" notitle,\
     "data/cmax.csv" using 1:($25)/1E9 with lines lc "dark-blue" title 'Gift',\
     "data/cmax.csv" using 1:($29)/1E9:($31)/1E9 with filledcurves lc "light-green" notitle,\
     "data/cmax.csv" using 1:($30)/1E9 with lines lc "dark-green" title 'Dis. Gift',\
     "data/cmax.csv" using 1:($39)/1E9:($41)/1E9 with filledcurves lc "light-red" notitle,\
     "data/cmax.csv" using 1:($40)/1E9 with lines lc "dark-red" title 'LP',\
     "data/cmax.csv" using 1:($34)/1E9:($36)/1E9 with filledcurves lc "plum" notitle,\
     "data/cmax.csv" using 1:($35)/1E9 with lines lc "dark-plum" title 'Random'
set output 'time2DerrorFlowtime.pdf'
plot "data/flowtime.csv" using 1:($24)/1E9:($26)/1E9 with filledcurves lc "light-blue" notitle,\
     "data/flowtime.csv" using 1:($25)/1E9 with lines lc "dark-blue" title 'Gift',\
     "data/flowtime.csv" using 1:($29)/1E9:($31)/1E9 with filledcurves lc "light-green" notitle,\
     "data/flowtime.csv" using 1:($30)/1E9 with lines lc "dark-green" title 'Dis. Gift',\
     "data/flowtime.csv" using 1:($39)/1E9:($41)/1E9 with filledcurves lc "light-red" notitle,\
     "data/flowtime.csv" using 1:($40)/1E9 with lines lc "dark-red" title 'LP',\
     "data/flowtime.csv" using 1:($34)/1E9:($36)/1E9 with filledcurves lc "plum" notitle,\
     "data/flowtime.csv" using 1:($35)/1E9 with lines lc "dark-plum" title 'Random'

