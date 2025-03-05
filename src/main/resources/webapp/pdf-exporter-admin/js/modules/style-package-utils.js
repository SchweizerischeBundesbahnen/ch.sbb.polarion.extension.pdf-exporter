const StylePackageUtils = {
    adjustWeight: function (input) {
        input = input || this; // input can be provided explicitly or implicitly during the listener call
        let value = parseFloat(input.value); // Get the current value and convert to float

        // 1. If the number > 100, set the value to 100
        if (value > 100) {
            value = 100;
        }

        // 2. If the decimal part contains more than 1 cipher, round to 1 decimal
        if (value % 1 !== 0) { // Check if there's a decimal part
            value = parseFloat(value.toFixed(1)); // Round to 1 decimal place
        }

        // 3. If the number doesn't fit the pattern NNN.N, set the value to 50
        const regex = /^\d{1,3}(\.\d)?$/;
        if (!regex.test(value)) {
            value = 50;
        }

        // Set the modified value back to the input field
        input.value = value;
    }
}

export default StylePackageUtils;
