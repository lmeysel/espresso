cd .\documentation\report

del main.aux
pdflatex main.tex
bibtex main
pdflatex main.tex
pdflatex main.tex