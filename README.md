# fluffy-dollop
A project of embedded course. This is a Clipboard sharing utility
(between android and linux) via communication with server using websockets.
The server supports the following [api](server/spec.md).

# Install

### Android
[![android build status](https://travis-ci.org/Frefreak/fluffy-dollop.svg)](https://travis-ci.org/Frefreak/fluffy-dollop)

In folder **android**. This is an **android-studio** project. Api 23 or above.

### Server & Linux
In folder **server** and **linux**. These are written in **Haskell** and you
need [stack](https://github.com/commercialhaskell/stack) to build it.
After you have successfully installed `stack`, run the following commands:
```bash
    $ stack init # only if you never run this before
    $ cd server # or "cd linux"
    $ stack setup # this might take some time depending on how many dependencies have you installed before
    $ stack build
    $ stack install # (optional) copy executable to some place, read the info generated by stack
```
### Chrome extension
In folder **chrome**.

1. open [`chrome://extensions`](chrome://extensions/)
2. make sure `Developer mode` in the upper right corner is checked.
3. Click `Load unpacked extension` button, navigate to select the **chrome** folder.
4. The extension should have be loaded. Currently only `post`, `login` and `register` are implemented.

**Note**: Syncing (No `alert`) is implicit to be called after login and
everytime the plugin reloaded. The `reSync` in popup menu is used only when
you find the connection might be lost, and no `alert`.

# TODO
- ~~create a simple web interface on server side~~
- ~~polish simple web interface with some css~~
- release some binary (probably try Travis CI for deploy apk)
- ~~create a simple chrome extension to right click and post to server (if have time)~~
- ~~change `ping` format which require token, so server can detect inactive token and erase it itself (similar to perform a `logout`)~~
- make android client more stable (bug fixes)
