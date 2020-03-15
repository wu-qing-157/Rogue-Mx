#include "malloc.h"
#include "stdio.h"
#include "string.h"

void *__malloc__array__(int size, int length) {
    void *ret = malloc(size + 4);
    *((int *) ret) = length;
    return ret + 4;
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
    int length = r - l;
    char *ret = malloc(5 + length) + 4;
    *((int *) (ret - 4)) = length;
    s += l;
    for (int i = 0; i < length; i++) ret[i] = s[i];
    ret[length] = 0;
    return ret;
}

char *__string__concatenate__(char *s, char *t) {
    int ls = *((int *) (s - 4)), tot = *((int *) (t - 4)) + ls;
    t -= ls;
    char *ret = malloc(5 + tot) + 4;
    *((int *) (ret - 4)) = tot;
    int i = 0;
    for (; i < ls; i++) ret[i] = s[i];
    for (; i < tot; i++) ret[i] = t[i];
    ret[tot] = 0;
    return ret;
}

char __string__equal__(char *s, char *t) {
    for (; ; s++, t++)
        if (*s != *t) return 0;
        else if (*s == '\0') return 1;
}

char __string__neq__(char *s, char *t) {
    for (; ; s++, t++)
        if (*s != *t) return 1;
        else if (*s == '\0') return 0;
}

char __string__less__(char *s, char *t) {
    for (; ; s++, t++)
        if (*s < *t) return 1;
        else if (*s > *t) return 0;
        else if (*s == '\0') return 0;
}

char __string__leq__(char *s, char *t) {
    for (; ; s++, t++)
        if (*s < *t) return 1;
        else if (*s > *t) return 0;
        else if (*s == '\0') return 1;
}

char __string__greater__(char *s, char *t) {
    for (; ; s++, t++)
        if (*s < *t) return 0;
        else if (*s > *t) return 1;
        else if (*s == '\0') return 0;
}

char __string__geq__(char *s, char *t) {
    for (; ; s++, t++)
        if (*s < *t) return 0;
        else if (*s > *t) return 1;
        else if (*s == '\0') return 1;
}

int __array__size__(void *a) {
    return *((int *) (a - 4));
}
