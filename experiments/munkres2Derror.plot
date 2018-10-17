set terminal pdfcairo
set datafile separator ","
set style fill solid border rgb "black"
set style fill transparent solid 0.1 noborder
set auto x
set auto y
set grid
set ticslevel 0
set style data lines
set xlabel "n workers / n tasks"
set ylabel "Flowtime"
set log y
set key center right
set output 'figures/munkres2DerrorFlowtime.pdf'
plot  "data/munkres.csv" using 1:3:5 with filledcurves lc "light-blue" notitle,\
      "data/munkres.csv" using 1:4 with lines dt 3 lc "dark-blue" title 'Swap',\
      "data/munkres.csv" using 1:8:10 with filledcurves lc "light-magenta" notitle,\
      "data/munkres.csv" using 1:9 with lines dt 7 lc "dark-magenta" title 'DisSwap',\
      "data/munkres.csv" using 1:13:15 with filledcurves lc "light-red" notitle,\
      "data/munkres.csv" using 1:14 with lines dt 5 lc "dark-red" title 'Munkres'
set output 'figures/munkres2DerrorTime.pdf'
set ylabel "Time (s)"
plot "data/munkres.csv" using 1:($18)/1E9:($20)/1E9 with filledcurves lc "light-blue" notitle,\
     "data/munkres.csv" using 1:($19)/1E9 with lines dt 3 lc "dark-blue" title 'Swap',\
     "data/munkres.csv" using 1:($23)/1E9:($25)/1E9 with filledcurves lc "light-magenta" notitle,\
     "data/munkres.csv" using 1:($24)/1E9 with lines dt 7 lc "dark-magenta" title 'DisSwap',\
     "data/munkres.csv" using 1:($28)/1E9:($30)/1E9 with filledcurves lc "light-red" notitle,\
     "data/munkres.csv" using 1:($29)/1E9 with lines dt 5 lc "dark-red" title 'Munkres'
