## Aurora Java Minecraft Server Plugin

### Features

- Mines
- Scoreboards
- etc TODO

#### Run Instructions
Download Paper 1.21

Add a start.bat file, with:

Add a new directory LocalServer

Run Maven build, which puts plugin in LocalServer, so make sure dir is created

`cd LocalServer` then run `./start.bat`

if `start.bat` fails, go to `eula.txt` and change `false` to `true`

rerun `start.bat`, join `127.0.0.1` on minecraft in server list

to stop server, first write `stop`, then `CTRL + C` after server starts to reboot