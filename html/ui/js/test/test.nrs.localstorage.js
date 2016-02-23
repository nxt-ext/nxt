QUnit.module("nxt.localstorage");

function init() {
    NRS.storageDrop("items");
    var data = [];
    data.push({"mykey1": "myvalue1"});
    data.push({"mykey2": "myvalue2"});
    data.push({"mykey3": "myvalue3"});
    NRS.storageInsert("items", data, function() {});
}

QUnit.test("select", function (assert) {
    init();
    NRS.storageSelect("items", null, function(error, response) {
        assert.equal(error, null);
        assert.equal(response.length, 3);
        assert.equal(response[0]["mykey1"], "myvalue1");
        assert.equal(response[1]["mykey2"], "myvalue2");
        assert.equal(response[2]["mykey3"], "myvalue3");
    });
    NRS.storageSelect("items", ["mykey1", "mykey3"], function(error, response) {
        assert.equal(error, null);
        assert.equal(response.length, 2);
        assert.equal(response[0], "myvalue1");
        assert.equal(response[1], "myvalue3");
    })
});

QUnit.test("update", function (assert) {
    init();
    NRS.storageUpdate("items", "myvalue22", ["mykey2"], function() {
        NRS.storageSelect("items", null, function(error, response) {
            assert.equal(error, null);
            assert.equal(response.length, 3);
            assert.equal(response[1]["mykey2"], "myvalue22");
        })
    });
});

QUnit.test("delete", function (assert) {
    init();
    NRS.storageDelete("items", ["mykey2"], function() {
        NRS.storageSelect("items", null, function(error, response) {
            assert.equal(error, null);
            assert.equal(response.length, 2);
            assert.equal(response[0]["mykey1"], "myvalue1");
            assert.equal(response[1]["mykey3"], "myvalue3");
        })
    });
    NRS.storageDelete("items", ["mykey4"], function() {
        NRS.storageSelect("items", null, function(error, response) {
            assert.equal(error, null);
            assert.equal(response.length, 2);
            assert.equal(response[0]["mykey1"], "myvalue1");
            assert.equal(response[1]["mykey3"], "myvalue3");
        })
    });
    NRS.storageDelete("items", ["mykey3"], function() {
        NRS.storageSelect("items", null, function(error, response) {
            assert.equal(error, null);
            assert.equal(response.length, 1);
            assert.equal(response[0]["mykey1"], "myvalue1");
        })
    });
});
