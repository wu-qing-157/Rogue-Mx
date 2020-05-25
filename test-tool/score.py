with open('test.score') as f:
    a = f.read()
a = list(map(float, a.split('\n')[:-1]))
a.sort()
score = sum(a[1:-1]) / 8 * 10
print('Final score: {}'.format(score))
