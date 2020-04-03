#include "malloc.h"
#include "stdio.h"
#include "string.h"

struct string {
    int size;
    char *s;
};

typedef struct string *string;

const int s_size = sizeof(struct string);

inline string _malloc_s_(int size) {
    void *alloc = malloc(s_size + size);
    string cast = alloc;
    cast->s = alloc + s_size;
    return cast;
}

void *_malloc_a_(int size, int length) {
    void *ret = malloc(size + 4);
    *((int *) ret) = length;
    return ret + 4;
}

int _get_i_() {
    int a;
    scanf("%d", &a);
    return a;
}

string _get_s_() {
    string ret = _malloc_s_(257);
    scanf("%s", ret->s);
    ret->size = strlen(ret->s);
    return ret;
}

void _print_s_(struct string *str) {
    printf("%s", str->s);
}

void _println_s_(struct string *str) {
    puts(str->s);
}

void _print_i_(int i) {
    printf("%d", i);
}

void _println_i_(int i) {
    printf("%d\n", i);
}

char d[15];

string _to_str_(int i) {
    string ret = _malloc_s_(15);
    char *c = d, *r = ret->s;
    if (i < 0) {
        *(r++) = '-';
        i = -i;
    }
    if (i == 0) *(c++) = '0';
    while (i) {
        *(c++) = i % 10 + '0';
        i /= 10;
    }

}

char *_to_str_(int i) {
    char *s = malloc(15) + 15, *t;
    *(t = --s) = '\0';
    char neg = i < 0;
    if (neg) i = -i;
    if (i == 0) *--s = '0';
    while (i) {
        *--s = i % 10 + '0';
        i /= 10;
    }
    if (neg) *--s = '-';
    *((int *) (s - 4)) = t - s;
    return s;
}

char *_s_literal_(char *s, int l) {
    char *ret = malloc(l + 5) + 4;
    strcpy(ret, s);
    *((int *) (ret - 4)) = l;
    return ret;
}

int _s_length_(char *s) {
    return *((int *) (s - 4));
}

int _s_ord_(char *s, int index) {
    return s[index];
}

int _s_parse_(char *s) {
    int i;
    sscanf(s, "%d", &i);
    return i;
}

char *_s_substring_(char *s, int l, int r) {
    int length = r - l;
    char *ret = malloc(5 + length) + 4;
    *((int *) (ret - 4)) = length;
    s += l;
    for (int i = 0; i < length; i++) ret[i] = s[i];
    ret[length] = 0;
    return ret;
}

char *_s_concatenate_(char *s, char *t) {
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

char _s_equal_(char *s, char *t) {
    for (; ; s++, t++)
        if (*s != *t) return 0;
        else if (*s == '\0') return 1;
}

char _s_neq_(char *s, char *t) {
    for (; ; s++, t++)
        if (*s != *t) return 1;
        else if (*s == '\0') return 0;
}

char _s_less_(char *s, char *t) {
    for (; ; s++, t++)
        if (*s < *t) return 1;
        else if (*s > *t) return 0;
        else if (*s == '\0') return 0;
}

char _s_leq_(char *s, char *t) {
    for (; ; s++, t++)
        if (*s < *t) return 1;
        else if (*s > *t) return 0;
        else if (*s == '\0') return 1;
}

char _s_greater_(char *s, char *t) {
    for (; ; s++, t++)
        if (*s < *t) return 0;
        else if (*s > *t) return 1;
        else if (*s == '\0') return 0;
}

char _s_geq_(char *s, char *t) {
    for (; ; s++, t++)
        if (*s < *t) return 0;
        else if (*s > *t) return 1;
        else if (*s == '\0') return 1;
}

int _a_size_(void *a) {
    return *((int *) (a - 4));
}
