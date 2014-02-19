# Ramit Arora 
# CS 414 MP1 Webcam Makefile
# University of Illinois
# refer Makefile tutorial: http://maemo.org/development/training/maemo_application_development_content/plain_html/node4/

#compiler
CC = gcc


#INC = -I

# additional flags
# -Wall: warnings
# -g: debugging
ADD_CFLAGS := -Wall -g


all_libs := gstreamer-interfaces-0.10 gtk+-2.0 gstreamer-0.10

# get the necessary flags for compiling
PKG_CFLAGS := $(shell pkg-config --cflags $(all_libs))

# get the necessary flags for linking
PKG_LDFLAGS := $(shell pkg-config --libs $(all_libs))

# combining the flags (so that CFLAGS/LDFLAGS from the command line
# still work).
CFLAGS  := $(PKG_CFLAGS) $(ADD_CFLAGS) $(CFLAGS)
LDFLAGS := $(PKG_LDFLAGS) $(LDFLAGS)

.PHONY : all
all: clean webcam

webcam: callbacks.o custom_pipeline.o GTK_UI.o webcam.o
	$(CC) $^ -o $@ -ggdb $(LDFLAGS)

callbacks.o: callbacks.c
	$(CC) $(CFLAGS) -c $< -o $@	

custom_pipeline.o: custom_pipeline.c
	$(CC) $(CFLAGS) -c $< -o $@
	

GTK_UI.o: GTK_UI.c
	$(CC) $(CFLAGS) -c $< -o $@

webcam.o: webcam.c	
	$(CC) $(CFLAGS) -c $< -o $@
	
.PHONY : clean
clean:
	-rm -f *.o webcam
