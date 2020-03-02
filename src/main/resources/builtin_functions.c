#include "malloc.h"
#include "stdio.h"
#include "string.h"

void *__malloc__(int length) {
    return malloc(length);
}

int __getInt__() {
    int a;
    scanf("%d", &a);
    return a;
}

char *__getString__() {
    char *s = malloc(261) + 4;
    scanf("%s", s);
    *((int *) (s - 4)) = strlen(s);
    return s;
}

void __print__(char *s) {
    printf("%s", s);
}

void __println__(char *s) {
    printf("%s\n", s);
}

void __printInt__(int i) {
    printf("%d", i);
}

void __printlnInt__(int i) {
    printf("%d\n", i);
}

char *__toString__(int i) {
    char *s = malloc(15) + 4;
    sprintf(s, "%d", i);
    *((int *) (s - 4)) = strlen(s);
    return s;
}

int __string__length__(char *s) {
    return *((int *) (s - 4));
}

int __string__ord__(int index, char *s) {
    return s[index];
}

int __string__parseInt__(char *s) {
    int i;
    sscanf(s, "%d", &i);
    return i;
}

char *__string__substring__(int l, int r, char *s) {
    int length = r - l + 1;
    char *ret = malloc(5 + length) + 4;
    *((int *) (ret - 4)) = length;
    for (int i = 0, j = l; j <= r; i++, j++) ret[i] = s[j];
    ret[length] = 0;
    return ret;
}

int __array__size__(void *a) {
    return *((int *) (a - 4));
}

void __empty__() {
}
