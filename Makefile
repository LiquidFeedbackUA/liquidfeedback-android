all:

icons:
	rsvg-convert -w 36 -h 36 -o res/drawable-ldpi/ic_launcher.png ic_launcher.svg
	rsvg-convert -w 48 -h 48 -o res/drawable-mdpi/ic_launcher.png ic_launcher.svg
	rsvg-convert -w 72 -h 72 -o res/drawable-hdpi/ic_launcher.png ic_launcher.svg
	rsvg-convert -w 96 -h 96 -o res/drawable-xhdpi/ic_launcher.png ic_launcher.svg
