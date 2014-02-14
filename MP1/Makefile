# Ramit Arora
# CS 414 MP1
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
all: player

player: player.o
	$(CC) $^ -o $@ $(LDFLAGS)
	
player.o: player.c
	$(CC) $(CFLAGS) -c $< -o $@
	
.PHONY : clean
clean:
	-rm -f *.o player
