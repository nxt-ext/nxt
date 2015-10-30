QUnit.test("convertToNXT", function (assert) {
    assert.equal(NRS.convertToNXT(200000000), "2", "whole");
    assert.equal(NRS.convertToNXT(20000000), "0.2", "fraction");
    assert.equal(NRS.convertToNXT(-200000000), "-2", "negative");
    assert.equal(NRS.convertToNXT(-20000000), "-0.2", "fraction.negative");
    assert.equal(NRS.convertToNXT(-220000000), "-2.2", "whole.fraction.negative");
    assert.equal(NRS.convertToNXT(2), "0.00000002", "nqt");
    assert.equal(NRS.convertToNXT(-2), "-0.00000002", "nqt.negative");
    assert.equal(NRS.convertToNXT(new BigInteger(String(2))), "0.00000002", "input.object");
    assert.equal(NRS.convertToNXT("hi"), "0.00000188", "alphanumeric"); // strange behavior of BigInteger don't do that
    assert.throws(function () {
        NRS.convertToNXT(null);
    }, {
        "message": "Cannot read property 'compareTo' of null",
        "name": "TypeError"
    }, "null.value");
});

QUnit.test("format", function (assert) {
    assert.equal(NRS.format("12345"), "12&#39;345", "escaped");
    assert.equal(NRS.format("12345", true), "12'345", "not.escaped");
    assert.equal(NRS.format("-12345", false), "-12&#39;345", "neg");
    assert.equal(NRS.format("-12345", true), "-12'345", "neg.not.escaped");
    assert.equal(NRS.format("-12345.67", true), "-12'345'.67", "decimal.not.good"); // bug ?
    assert.equal(NRS.format({ amount: 1234, negative: '-', mantissa: ".567"}, true), "-1'234.567", "object");
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

QUnit.test("formatVolume", function (assert) {
    assert.equal(NRS.formatVolume(1), "1 B", "byte");
    assert.equal(NRS.formatVolume(1000), "1'000 B", "thousand");
    assert.equal(NRS.formatVolume(1024), "1 KB", "kilo");
    assert.equal(NRS.formatVolume(1000000), "977 KB", "million");
    assert.equal(NRS.formatVolume(1024*1024), "1 MB", "million");
    assert.equal(NRS.formatVolume(2*1024*1024 + 3*1024 + 4), "2 MB", "combination");
});

QUnit.test("formatWeight", function (assert) {
    assert.equal(NRS.formatWeight(1), "1", "byte");
    assert.equal(NRS.formatWeight(1000), "1&#39;000", "thousand");
    assert.equal(NRS.formatWeight(12345), "12&#39;345", "number");
});

QUnit.test("calculateOrderPricePerWholeQNT", function (assert) {
    assert.equal(NRS.calculateOrderPricePerWholeQNT(100000000, 0), "1", "no.decimals.one");
    assert.equal(NRS.calculateOrderPricePerWholeQNT(1, 4), "0.0001", "fraction");
    assert.equal(NRS.calculateOrderPricePerWholeQNT(-123400000000, 8), "-123400000000", "eight.decimals");
    assert.equal(NRS.calculateOrderPricePerWholeQNT(-123400000000, 4), "-12340000", "four.decimals");
    assert.equal(NRS.calculateOrderPricePerWholeQNT(-123400000000, 0), "-1234", "no.decimals");
});

QUnit.test("formatOrderPricePerWholeQNT", function (assert) {
    assert.equal(NRS.formatOrderPricePerWholeQNT(100000000, 0), "1", "no.decimals.one");
    assert.equal(NRS.formatOrderPricePerWholeQNT(1, 4), "0.0001", "fraction");
    assert.equal(NRS.formatOrderPricePerWholeQNT(-123400000000, 8), "-123'400'000'000".escapeHTML(), "eight.decimals");
    assert.equal(NRS.formatOrderPricePerWholeQNT(-123400000000, 4), "-12'340'000".escapeHTML(), "four.decimals");
    assert.equal(NRS.formatOrderPricePerWholeQNT(-123400000000, 0), "-1'234".escapeHTML(), "no.decimals");
});

QUnit.test("calculatePricePerWholeQNT", function (assert) {
    assert.equal(NRS.calculatePricePerWholeQNT(100000000, 0), "100000000", "no.decimals.one");
    assert.equal(NRS.calculatePricePerWholeQNT(100000000, 4), "10000", "four.decimals");
    assert.equal(NRS.calculatePricePerWholeQNT(100000000, 8), "1", "eight.decimals");
    assert.equal(NRS.calculatePricePerWholeQNT(-123400000000, 8), "-1234".escapeHTML(), "eight.decimals");
    assert.equal(NRS.calculatePricePerWholeQNT(-123400000000, 4), "-12340000".escapeHTML(), "four.decimals");
    assert.equal(NRS.calculatePricePerWholeQNT(-123400000000, 0), "-123400000000".escapeHTML(), "no.decimals");
    assert.throws(function () {
        NRS.calculatePricePerWholeQNT(100000001, 8);
    }, "Invalid input.", "invalid.input");
});

QUnit.test("calculateOrderTotalNQT", function (assert) {
    assert.equal(NRS.calculateOrderTotalNQT(12, 34), "408", "multiplication");
});

QUnit.test("calculateOrderTotal", function (assert) {
    assert.equal(NRS.calculateOrderTotal(12, 34), "0.00000408", "multiplication");
});

QUnit.test("calculatePercentage", function (assert) {
    assert.equal(NRS.calculatePercentage(6, 15), "40.00", "pct1");
    assert.equal(NRS.calculatePercentage(5, 15), "33.33", "pct1");
    assert.equal(NRS.calculatePercentage(10, 15), "66.67", "pct3");
    assert.equal(NRS.calculatePercentage(10, 15, 0), "66.66", "pct3.round0");
    assert.equal(NRS.calculatePercentage(10, 15, 1), "66.67", "pct3.round1");
    assert.equal(NRS.calculatePercentage(10, 15, 2), "66.67", "pct3.round2");
    assert.equal(NRS.calculatePercentage(10, 15, 3), "66.67", "pct3.round3");
});

QUnit.test("amountToPrecision", function (assert) {
    assert.equal(NRS.amountToPrecision(12, 0), "12", "multiplication");
    assert.equal(NRS.amountToPrecision(12., 0), "12", "multiplication");
    assert.equal(NRS.amountToPrecision(12.0, 0), "12", "multiplication");
    assert.equal(NRS.amountToPrecision(12.345600, 4), "12.3456", "multiplication");
    assert.equal(NRS.amountToPrecision(12.3456, 4), "12.3456", "multiplication");
    assert.equal(NRS.amountToPrecision(12.3456, 3), "12.345", "multiplication");
    assert.equal(NRS.amountToPrecision(12.3456, 2), "12.34", "multiplication");
    assert.equal(NRS.amountToPrecision(12.3006, 2), "12.30", "multiplication");
});

QUnit.test("convertToNQT", function (assert) {
    assert.equal(NRS.convertToNQT(1), "100000000", "one");
    assert.equal(NRS.convertToNQT(1.), "100000000", "one.dot");
    assert.equal(NRS.convertToNQT(1.0), "100000000", "one.dot.zero");
    assert.equal(NRS.convertToNQT(.1), "10000000", "dot.one");
    assert.equal(NRS.convertToNQT(0.1), "10000000", "zero.dot.one");
    assert.equal(NRS.convertToNQT("0.00000001"), "1", "nqt");
    assert.throws(function () {
        NRS.convertToNQT(0.00000001); // since it's passed as 1e-8
    }, "Invalid input.", "invalid.input");
});
