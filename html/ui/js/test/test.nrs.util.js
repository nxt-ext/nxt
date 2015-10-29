QUnit.test("convertToNXT", function (assert) {
    assert.equal(NRS.convertToNXT(200000000), "2", "whole");
    assert.equal(NRS.convertToNXT(20000000), "0.2", "fraction");
    assert.equal(NRS.convertToNXT(-200000000), "-2", "negative");
    assert.equal(NRS.convertToNXT(-20000000), "-0.2", "fraction.negative");
    assert.equal(NRS.convertToNXT(-220000000), "-2.2", "whole.fraction.negative");
    assert.equal(NRS.convertToNXT(2), "0.00000002", "nqt");
    assert.equal(NRS.convertToNXT(-2), "-0.00000002", "nqt.negative");
    assert.equal(NRS.convertToNXT(new BigInteger(String(2))), "0.00000002", "input.object");
    assert.equal(NRS.convertToNXT("hi"), "0.00000188", "alphanumeric");
});

QUnit.test("format", function (assert) {
    assert.equal(NRS.format("12345", false), "12&#39;345", "nqt");
    assert.equal(NRS.format("12345", true), "12'345", "nqt");
    assert.equal(NRS.format("-12345", false), "-12&#39;345", "nqt");
    assert.equal(NRS.format("-12345", true), "-12'345", "nqt");
    assert.equal(NRS.format("-12345.67", true), "-12'345'.67", "nqt"); // bug ?
});

QUnit.test("formatAmount", function (assert) {
    assert.equal(NRS.formatAmount("12345", false, false), "0.00012345", "nqt");
    assert.equal(NRS.formatAmount("12345", true, false), "0.00012345", "nqt.rounding");
    assert.equal(NRS.formatAmount("1234500000", false, false), "12.345", "string");
    assert.equal(NRS.formatAmount("1234500000", true, false), "12.345", "string.no.rounding");
    assert.equal(NRS.formatAmount(12.345, false, false), "12.345", "number");
    assert.equal(NRS.formatAmount(12.345, true, false), "12.35", "number.rounding");
    assert.equal(NRS.formatAmount(12.343, true, false), "12.34", "number.rounding");
    assert.equal(NRS.formatAmount("123456700000", false, true), "1'234.567", "1000separator");
    assert.equal(NRS.formatAmount("123456700000000", true, true), "1'234'567", "nxt.rounding");
});
