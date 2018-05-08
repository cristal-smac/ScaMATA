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
set output 'time2DerrorCmaxGiftVsDisGift.pdf'
plot "data/cmax.csv" using 1:24:26 with filledcurves lc "light-blue" notitle,\
     "data/cmax.csv" using 1:25 with lines lc "dark-blue" title 'Gift',\
     "data/cmax.csv" using 1:29:31 with filledcurves lc "light-green" notitle,\
     "data/cmax.csv" using 1:30 with lines lc "dark-green" title 'Dis. Gift'
set output 'time2DerrorCmaxGiftVsLP.pdf'
plot "data/cmax.csv" using 1:24:26 with filledcurves lc "light-blue" notitle,\
     "data/cmax.csv" using 1:25 with lines lc "dark-blue" title 'Gift',\
     "data/cmax.csv" using 1:39:41 with filledcurves lc "light-red" notitle,\
     "data/cmax.csv" using 1:40 with lines lc "dark-red" title 'LP'
set output 'time2DerrorFlowtimeGiftVsDisGift.pdf'
plot "data/flowtime.csv" using 1:24:26 with filledcurves lc "light-blue" notitle,\
     "data/flowtime.csv" using 1:25 with lines lc "dark-blue" title 'Gift',\
     "data/flowtime.csv" using 1:29:31 with filledcurves lc "light-green" notitle,\
     "data/flowtime.csv" using 1:30 with lines lc "dark-green" title 'Dis. Gift'
set output 'time2DerrorFlowtimeGiftVsLP.pdf'
plot "data/flowtime.csv" using 1:24:26 with filledcurves lc "light-blue" notitle,\
     "data/flowtime.csv" using 1:25 with lines lc "dark-blue" title 'Gift',\
     "data/flowtime.csv" using 1:39:41 with filledcurves lc "light-red" notitle,\
     "data/flowtime.csv" using 1:40 with lines lc "dark-red" title 'LP'
