#!/bin/sh
java -cp classes:lib/*:conf -Dnxt.runtime.mode=desktop -Dnxt.runtime.dirProvider=nxt.env.DefaultDirProvider nxt.Nxt
