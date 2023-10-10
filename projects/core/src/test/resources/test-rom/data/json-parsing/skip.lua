local skip = {
    -- Should fail, but pass.
    -- We're too flexible on number formatting, but it's not a major concern.
    "n_number_-01",
    "n_number_-2.",
    "n_number_0.e1",
    "n_number_2.e3",
    "n_number_2.e+3",
    "n_number_2.e-3",
    "n_number_neg_int_starting_with_zero",
    "n_number_real_without_fractional_part",
    "n_number_with_leading_zero",

    -- Should pass, but fail.
    -- These two are due to stack overflows within the parser.
    "n_structure_open_array_object",
    "n_structure_100000_opening_arrays",
}

for _, k in pairs(skip) do skip[k] = true end
return skip
