QUnit.module("nxt.localstorage");

function init() {
    NRS.storageDeleteTable("items");
    var data = [];
    data.push({"mykey1": "myvalue1"});
    data.push({"mykey2": "myvalue2"});
    data.push({"mykey3": "myvalue3"});
    return data;
}
QUnit.test("insertSelect", function (assert) {
    var data = init();
    NRS.storageInsert("items", data, function() {});
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
