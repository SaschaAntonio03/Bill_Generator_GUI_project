CC      = gcc
CFLAGS  = -g -Wall \
          -F/usr/local/Frameworks \
          -I/usr/local/Frameworks/Tcl.framework/Headers \
          -I/usr/local/Frameworks/Tk.framework/Headers
LDFLAGS = -framework Tcl -framework Tk

SRCS    = database.c main.c
OBJS    = $(SRCS:.c=.o)
TARGET  = app

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) -o $@ $(OBJS) $(LDFLAGS)

%.o: %.c database.h
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	rm -f $(OBJS) $(TARGET)
