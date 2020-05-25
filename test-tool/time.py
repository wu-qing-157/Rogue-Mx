#!python

import sys

std = {
    'binary_tree': 689489878,
    'dijkstra': 172592188,
    'humble': 879242121,
    'kruskal': 144187049,
    'lca': 97359977,
    'lunatic': 2454305581,
    'maxflow': 44915424,
    'pi': 30400478,
    'segtree': 1021400514,
    'sha_1': 1429854858
}

with open('test.detail') as f:
    s = f.read()
index = s.find('time: ')
index += 6
time = ''
while s[index] >= '0':
    time += s[index]
    index += 1
print(time.ljust(15), end='')
time = int(time)
k = time / std[sys.argv[1]]
print(str(k).ljust(20), end='')
score = 10 - k * 6
print(str(score).ljust(25), end='')
with open('test.score', 'a') as f:
    f.write('{}\n'.format(min(max(score, 0), 5)))
