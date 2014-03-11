/* WebDB v1.0.0 - 1/21/2014
   http://github.com/haas85/webdb
   Copyright (c) 2014 Iñigo Gonzalez Vazquez <ingonza85@gmail.com> (@haas85) - Under MIT License */
(function() {
  var WebDB, indexedDB, webDB, webSQL, _mix, _typeOf;

  window.indexedDB = window.indexedDB || window.webkitIndexedDB || window.mozIndexedDB;

  webDB = (function() {
    webDB.prototype.db = null;

    function webDB(name, schema, version, size, callback) {
      var key, manager;
      this.name = name;
      this.schema = schema;
      this.version = version;
      this.size = size != null ? size : 5242880;
      try {
	      if (window.indexedDB) {
	        this.schema = (function() {
	          var _results;
	          _results = [];
	          for (key in this.schema) {
	            _results.push(key);
	          }
	          return _results;
	        }).call(this);
	        manager = new WebDB.indexedDB(this.name, this.schema, this.version, callback);
	      } else if (window.openDatabase) {
	        manager = new WebDB.webSQL(this.name, this.schema, this.version, this.size, callback);
	      } else {
		      throw "Browser does not have database support.";
	      }
	  } catch (e) {
		  throw e;
	  }
      if (!window.openDatabase && !window.indexedDB) {
        this.select = function() {
          throw "HTML5 Databases not supported";
        };
        this.insert = function() {
          throw "HTML5 Databases not supported";
        };
        this.update = function() {
          throw "HTML5 Databases not supported";
        };
        this["delete"] = function() {
          throw "HTML5 Databases not supported";
        };
        this.drop = function() {
          throw "HTML5 Databases not supported";
        };
        this.execute = function() {
          throw "HTML5 Databases not supported";
        };
        throw "HTML5 Databases not supported";
      }
      this.db = manager.db;
      this.select = function() {
        return manager.select.apply(manager, arguments);
      };
      this.insert = function() {
        return manager.insert.apply(manager, arguments);
      };
      this.update = function() {
        return manager.update.apply(manager, arguments);
      };
      this["delete"] = function() {
        return manager["delete"].apply(manager, arguments);
      };
      this.drop = function() {
        return manager.drop.apply(manager, arguments);
      };
      this.execute = function() {
        return manager.execute.apply(manager, arguments);
      };
    }

    return webDB;

  })();

  WebDB = window.WebDB = webDB;

  _mix = function(receiver, emitter) {
    var key, _results;
    _results = [];
    for (key in emitter) {
      _results.push(receiver[key] = emitter[key]);
    }
    return _results;
  };

  _typeOf = function(obj) {
    return Object.prototype.toString.call(obj).match(/[a-zA-Z] ([a-zA-Z]+)/)[1].toLowerCase();
  };

  indexedDB = (function() {
    var _check, _queryOp, _write;

    indexedDB.prototype.db = null;

    function indexedDB(name, schema, version, callback) {
      var openRequest,
        _this = this;
      if (version == null) {
        version = 1;
      }
      if (!window.indexedDB) {
        throw "IndexedDB not supported";
      }
      
      openRequest = window.indexedDB.open(name, version);
      
      openRequest.onsuccess = function(e) {
        _this.db = e.target.result;
        if (callback != null) {
          return callback.call(callback);
        }
      };
      
      openRequest.onupgradeneeded = function(e) {
        var options, table, _i, _len, _results;
        _this.db = e.target.result;
        options = {
          keyPath: "id",
          autoIncrement: true
        };
        _results = [];
        for (_i = 0, _len = schema.length; _i < _len; _i++) {
          table = schema[_i];
          if (!_this.db.objectStoreNames.contains(table)) {
            _results.push(_this.db.createObjectStore(table, options));
          } else {
            _results.push(void 0);
          }
        }
        return _results;
      };
      openRequest.onversionchange = function(e) {
        return console.log(e);
      };
    }

    indexedDB.prototype.select = function(table, query, callback) {
      if (query == null) {
        query = [];
      }
      return _queryOp(this.db, table, null, query, callback);
    };

    indexedDB.prototype.insert = function(table, data, callback) {
      var len, row, _i, _len, _results;
      if (_typeOf(data) === "object") {
        return _write(this, table, data, callback);
      } else {
        len = data.length;
        _results = [];
        for (_i = 0, _len = data.length; _i < _len; _i++) {
          row = data[_i];
          	
          _results.push(_write(this, table, row, function() {
            len--;
            if (len === 0 && (callback != null)) {
              return callback.call(callback, data.length);
            }
          }));
        }
        return _results;
      }
    };

    indexedDB.prototype.update = function(table, data, query, callback) {
      if (query == null) {
        query = [];
      }
      return _queryOp(this.db, table, data, query, function(result) {
        if (callback != null) {
          return callback.call(callback, result.length);
        }
      });
    };

    indexedDB.prototype["delete"] = function(table, query, callback) {
      var exception, result, store;
      if (query == null) {
        query = [];
      }
      try {
        result = 0;
        store = this.db.transaction([table], "readwrite").objectStore(table);
        return store.openCursor().onsuccess = function(e) {
          var cursor, element;
          cursor = e.target.result;
          if (cursor) {
            element = cursor.value;
            if (_check(element, query)) {
              result++;
              store["delete"](cursor.primaryKey);
            }
            return cursor["continue"]();
          } else {
            if (callback != null) {
              return callback.call(callback, result);
            }
          }
        };
      } catch (_error) {
        exception = _error;
        if (callback != null) {
          return callback.call(callback);
        }
      }
    };

    indexedDB.prototype.drop = function(table, callback) {
      var exception, store;
      try {
        store = this.db.transaction([table], "readwrite").objectStore(table);
        store.openCursor().onsuccess = function(e) {
          var cursor;
          cursor = e.target.result;
          if (cursor) {
            store["delete"](cursor.primaryKey);
            return cursor["continue"]();
          }
        };
        if (callback != null) {
          return callback.call(callback);
        }
      } catch (_error) {
        exception = _error;
        if (callback != null) {
          return callback.call(callback);
        }
      }
    };

    indexedDB.prototype.execute = function(sql, callbacl) {
      return "";
    };

    _write = function(_this, table, data, callback) {
      var request, store;
      store = _this.db.transaction([table], "readwrite").objectStore(table);
      request = store.add(data);
      request.onerror = function(e) {
        if (callback != null) {
          return callback.call(callback, null);
        }
      };
      return request.onsuccess = function(result) {
        if (callback != null) {
          return callback.call(callback, 1);
        }
      };
    };

    _check = function(element, query) {
      var key, result, stmt, _i, _len;
      if (query == null) {
        query = [];
      }
      if (query.length === 0) {
        return true;
      }
      for (_i = 0, _len = query.length; _i < _len; _i++) {
        stmt = query[_i];
        result = true;
        for (key in stmt) {
          if (typeof stmt[key] == "string") {
	         if (String(element[key]).toLowerCase() !== stmt[key].toLowerCase()) {
		         result = false;
		         break;
	         } 
          } else {
	          if (element[key] !== stmt[key]) {
	            result = false;
	            break;
	          }
          }
        }
        if (result === true) {
          return true;
        }
      }
      return false;
    };

    _queryOp = function(db, table, data, query, callback) {    	
      var op, result;
      if (query == null) {
        query = [];
      }
      result = [];
      op = data != null ? "readwrite" : "readonly";
      return db.transaction([table], op).objectStore(table).openCursor().onsuccess = function(e) {
        var cursor, element;
        cursor = e.target.result;
        if (cursor) {
          element = cursor.value;
          if (_check(element, query)) {
            if (data != null) {
              _mix(element, data);
              _mix(cursor.value, data);
              cursor.update(cursor.value);
            }
            result.push(element);
          }
          return cursor["continue"]();
        } else {
          if (callback != null) {
            return callback.call(callback, result);
          }
        }
      };
    };

    return indexedDB;

  })();

  WebDB.indexedDB = indexedDB;

  webSQL = (function() {
    var _insert, _queryToSQL, _setValue, _this;

    webSQL.prototype.db = null;

    _this = null;

    function webSQL(name, schema, version, size, callback) {
      var row, sql, table, _tables;
      if (size == null) {
        size = 5;
      }
      if (!window.openDatabase) {
        throw "WebSQL not supported";
      }
      size = size * 1024 * 1024;
      this.db = window.openDatabase(name, version, "", size);
      _tables = 0;
      for (table in schema) {
        sql = "CREATE TABLE IF NOT EXISTS " + table + " (";
        for (row in schema[table]) {
          sql += "\"" + row + "\" " + schema[table][row] + ",";
        }
        sql = sql.substring(0, sql.length - 1) + ")";
        _tables++;
        _this = this;
        this.execute(sql, [], function() {
          _tables--;
          if (_tables === 0 && (callback != null)) {
            return callback.call(callback);
          }
        });
      }
    }

    webSQL.prototype.select = function(table, query, callback) {
      var sql;
      if (query == null) {
        query = [];
      }
      sql = ("SELECT * FROM " + table) + _queryToSQL(query);
      
      return this.execute(sql, [], callback);
    };

    webSQL.prototype.insert = function(table, data, callback) {
      var len, result, row, _i, _len, _results;
      if (_typeOf(data) === "object") {
        return _insert(table, data, callback);
      } else {
        len = data.length;
        result = 0;
        _results = [];
        for (_i = 0, _len = data.length; _i < _len; _i++) {
          row = data[_i];
          _results.push(_insert(table, row, function(row) {
            len--;
            result++;
            if (len === 0 && (callback != null)) {
              return callback.call(callback, result);
            }
          }));
        }
        return _results;
      }
    };

    webSQL.prototype.update = function(table, data, query, callback) {
      var key, sql;
      if (query == null) {
        query = [];
      }
      sql = "UPDATE " + table + " SET ";
      for (key in data) {
        sql += "\"" + key + "\" = " + (_setValue(data[key])) + ", ";
      }
      sql = sql.substring(0, sql.length - 2) + _queryToSQL(query);
      return this.execute(sql, [], callback);
    };

    webSQL.prototype["delete"] = function(table, query, callback) {
      var sql;
      if (query == null) {
        query = [];
      }
      sql = "DELETE FROM " + table + " " + (_queryToSQL(query));
      return this.execute(sql, [], callback);
    };

    webSQL.prototype.drop = function(table, callback) {
      return this.execute("DROP TABLE IF EXISTS " + table, [], callback);
    };

    webSQL.prototype.execute = function(sql, data, callback) {    	
      if (!this.db) {
        throw "Database not initializated";
      } else {      	
        return this.db.transaction(function(tx) {
          return tx.executeSql(sql, data, function(transaction, resultset) {
            var i, result;
            result = [];
            if (sql.indexOf("SELECT") !== -1) {
              result = (function() {
                var _i, _ref, _results;
                _results = [];
                for (i = _i = 0, _ref = resultset.rows.length; 0 <= _ref ? _i < _ref : _i > _ref; i = 0 <= _ref ? ++_i : --_i) {
                  _results.push(resultset.rows.item(i));
                }
                return _results;
              })();
              if (callback != null) {
                return callback.call(callback, result);
              }
            } else {
              if (callback != null) {
                return callback.call(callback, resultset.rowsAffected);
              }
            }
          });
        });
      }
    };

    _insert = function(table, row, callback) {
      var data, key, sql;
      var arr = [];
      sql = "INSERT INTO " + table + " (";
      data = "(";
      for (key in row) {
        sql += "\"" + key + "\", ";
        data += "?, ";
        arr.push(row[key]);
      }
      sql = sql.substring(0, sql.length - 2) + ") ";
      data = data.substring(0, data.length - 2) + ") ";
      sql += " VALUES " + data;
            
      return _this.execute(sql, arr, callback);
    };

    _queryToSQL = function(query) {
      var elem, or_stmt, sql, value, _i, _len;
      if (query.length > 0) {
        sql = " WHERE (";
        for (_i = 0, _len = query.length; _i < _len; _i++) {
          elem = query[_i];
          for (or_stmt in elem) {
            value = elem[or_stmt];
            sql += "\"" + or_stmt + "\" = " + (_setValue(value)) + " AND ";
          }
          sql = sql.substring(0, sql.length - 5) + ") OR (";
        }
        return sql.substring(0, sql.length - 5);
      } else {
        return "";
      }
    };

    _setValue = function(value) {
      if (typeof(value) != "number") {
        return "'" + value + "'";
      } else {
        return value;
      }
    };

    return webSQL;

  })();

  WebDB.webSQL = webSQL;

}).call(this);