import ctypes, ctypes.util, sys, time, os
x11 = ctypes.CDLL(ctypes.util.find_library('X11'))
xtst = ctypes.CDLL(ctypes.util.find_library('Xtst'))
x11.XOpenDisplay.restype = ctypes.c_void_p
d = x11.XOpenDisplay(None)
if not d:
    sys.exit("cannot open display")
x11.XKeysymToKeycode.argtypes = [ctypes.c_void_p, ctypes.c_ulong]
x11.XStringToKeysym.argtypes = [ctypes.c_char_p]
x11.XStringToKeysym.restype = ctypes.c_ulong
x11.XFlush.argtypes = [ctypes.c_void_p]
xtst.XTestFakeKeyEvent.argtypes = [ctypes.c_void_p, ctypes.c_uint, ctypes.c_int, ctypes.c_ulong]
xtst.XTestFakeMotionEvent.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_int, ctypes.c_int, ctypes.c_ulong]
xtst.XTestFakeButtonEvent.argtypes = [ctypes.c_void_p, ctypes.c_uint, ctypes.c_int, ctypes.c_ulong]
def move(x, y):
    xtst.XTestFakeMotionEvent(d, -1, x, y, 0); x11.XFlush(d)
def click(x, y, button=1):
    move(x, y); time.sleep(0.15)
    xtst.XTestFakeButtonEvent(d, button, 1, 0); x11.XFlush(d); time.sleep(0.08)
    xtst.XTestFakeButtonEvent(d, button, 0, 0); x11.XFlush(d)
def key(name, hold=0.08):
    code = x11.XKeysymToKeycode(d, x11.XStringToKeysym(name.encode()))
    xtst.XTestFakeKeyEvent(d, code, 1, 0); x11.XFlush(d); time.sleep(hold)
    xtst.XTestFakeKeyEvent(d, code, 0, 0); x11.XFlush(d)
x11.XKeycodeToKeysym.argtypes = [ctypes.c_void_p, ctypes.c_uint, ctypes.c_int]
x11.XKeycodeToKeysym.restype = ctypes.c_ulong
NAMES = {' ': 'space', '/': 'slash', '~': 'asciitilde', '@': 'at', '.': 'period', '-': 'minus', ':': 'colon', '_': 'underscore', '=': 'equal', ',': 'comma'}
def typ(text):
    shift = x11.XKeysymToKeycode(d, x11.XStringToKeysym(b'Shift_L'))
    for ch in text:
        name = NAMES.get(ch, ch)
        sym = x11.XStringToKeysym(name.encode())
        code = x11.XKeysymToKeycode(d, sym)
        need_shift = x11.XKeycodeToKeysym(d, code, 0) != sym
        if need_shift:
            xtst.XTestFakeKeyEvent(d, shift, 1, 0); x11.XFlush(d); time.sleep(0.03)
        xtst.XTestFakeKeyEvent(d, code, 1, 0); x11.XFlush(d); time.sleep(0.04)
        xtst.XTestFakeKeyEvent(d, code, 0, 0); x11.XFlush(d)
        if need_shift:
            xtst.XTestFakeKeyEvent(d, shift, 0, 0); x11.XFlush(d)
        time.sleep(0.05)
cmd = sys.argv[1]
if cmd == 'move': move(int(sys.argv[2]), int(sys.argv[3]))
elif cmd == 'click': click(int(sys.argv[2]), int(sys.argv[3]))
elif cmd == 'key':
    for k in sys.argv[2:]:
        key(k); time.sleep(0.25)
elif cmd == 'type': typ(' '.join(sys.argv[2:]))
