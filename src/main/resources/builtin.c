#include "malloc.h"
#include "stdio.h"
#include "string.h"

struct string {
    int length;
    char s[0];
};

typedef struct string *string;

typedef struct array *array;

int *_malloc_o_(int size) {
    return malloc(size);
}

string _malloc_s_(int length) {
    string ret = malloc(sizeof(int) + length + 1);
    ret->length = length;
    ret->s[length] = '\0';
    return ret;
}

int *_malloc_a_(int size) {
    int *ret = malloc(sizeof(int) + size * 4);
    *ret = size;
    return ret + 1;
}

int _get_i_() {
    int a;
    scanf("%d", &a);
    return a;
}

char input[257];

string _get_s_() {
    scanf("%s", input);
    string ret = _malloc_s_(strlen(input));
    strcpy(ret->s, input);
    return ret;
}

void _print_s_(string str) {
    printf("%s", str->s);
}

void _println_s_(string str) {
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
    char *c = d;
    int neg = 0;
    if (i < 0) {
        neg = 1;
        i = -i;
    }
    if (i == 0) *(c++) = '0';
    while (i) {
        *(c++) = i % 10 + '0';
        i /= 10;
    }
    int length = c - d;
    string ret = _malloc_s_(neg + length);
    char *t = ret->s;
    if (neg) *(t++) = '-';
    while (c != d) *(t++) = *--c;
    return ret;
}

int _s_length_(string s) {
    return s->length;
}

int _s_ord_(string s, int index) {
    return s->s[index];
}

int _s_parse_(string s) {
    int i;
    sscanf(s->s, "%d", &i);
    return i;
}

string _s_substring_(string s, int l, int r) {
    string ret = _malloc_s_(r - l);
    for (int i = 0, j = l; j < r; i++, j++) ret->s[i] = s->s[j];
    return ret;
}

string _s_concatenate_(string s, string t) {
    string ret = _malloc_s_(s->length + t->length);
    int i = 0;
    for (int j = 0; j < s->length; i++, j++) ret->s[i] = s->s[j];
    for (int k = 0; k < t->length; i++, k++) ret->s[i] = t->s[k];
    return ret;
}

char _s_equal_(string s, string t) {
    for (char *a = s->s, *b = t->s; ; a++, b++) {
        if (*a != *b) return 0;
        if (*a == '\0') return 1;
    }
}

char _s_neq_(string s, string t) {
    for (char *a = s->s, *b = t->s; ; a++, b++) {
        if (*a != *b) return 1;
        if (*a == '\0') return 0;
    }
}

char _s_less_(string s, string t) {
    for (char *a = s->s, *b = t->s; ; a++, b++) {
        if (*a < *b) return 1;
        if (*a > *b || *a == '\0') return 0;
    }
}

char _s_leq_(string s, string t) {
    for (char *a = s->s, *b = t->s; ; a++, b++) {
        if (*a > *b) return 0;
        if (*a < *b || *a == '\0') return 1;
    }
}

char _s_greater_(string s, string t) {
    for (char *a = s->s, *b = t->s; ; a++, b++) {
        if (*a > *b) return 1;
        if (*a < *b || *a == '\0') return 0;
    }
}

char _s_geq_(string s, string t) {
    for (char *a = s->s, *b = t->s; ; a++, b++) {
        if (*a < *b) return 0;
        if (*a > *b || *a == '\0') return 1;
    }
}
