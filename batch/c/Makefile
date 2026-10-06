# URPWD01 電力日報バッチ  (RHEL6 / gcc)
CC      = gcc
CFLAGS  = -O2 -Wall -std=c89
TARGET  = work/urpwd01

all: $(TARGET)

$(TARGET): urpwd01.c
	mkdir -p work
	$(CC) $(CFLAGS) -o $@ urpwd01.c

clean:
	rm -rf work
