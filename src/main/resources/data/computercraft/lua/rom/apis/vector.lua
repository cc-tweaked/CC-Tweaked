--- The vector API provides methods to create and manipulate vectors.
--
-- An introduction to vectors can be found on [Wikipedia][wiki].
--
-- [wiki]: http://en.wikipedia.org/wiki/Euclidean_vector
--
-- @module vector
-- @since 1.31

--- A 3-dimensional vector, with `x`, `y`, and `z` values.
--
-- This is suitable for representing both position and directional vectors.
--
-- @type Vector
local vector = {
    --- Adds two vectors together.
    --
    -- @tparam Vector self The first vector to add.
    -- @tparam Vector o The second vector to add.
    -- @treturn Vector The resulting vector
    -- @usage v1:add(v2)
    -- @usage v1 + v2
    add = function(self, o)
        return vector.new(
            self.x + o.x,
            self.y + o.y,
            self.z + o.z
        )
    end,

    --- Subtracts one vector from another.
    --
    -- @tparam Vector self The vector to subtract from.
    -- @tparam Vector o The vector to subtract.
    -- @treturn Vector The resulting vector
    -- @usage v1:sub(v2)
    -- @usage v1 - v2
    sub = function(self, o)
        return vector.new(
            self.x - o.x,
            self.y - o.y,
            self.z - o.z
        )
    end,

    --- Multiplies a vector by a scalar value.
    --
    -- @tparam Vector self The vector to multiply.
    -- @tparam number m The scalar value to multiply with.
    -- @treturn Vector A vector with value `(x * m, y * m, z * m)`.
    -- @usage v:mul(3)
    -- @usage v * 3
    mul = function(self, m)
        return vector.new(
            self.x * m,
            self.y * m,
            self.z * m
        )
    end,

    --- Divides a vector by a scalar value.
    --
    -- @tparam Vector self The vector to divide.
    -- @tparam number m The scalar value to divide by.
    -- @treturn Vector A vector with value `(x / m, y / m, z / m)`.
    -- @usage v:div(3)
    -- @usage v / 3
    div = function(self, m)
        return vector.new(
            self.x / m,
            self.y / m,
            self.z / m
        )
    end,

    --- Negate a vector
    --
    -- @tparam Vector self The vector to negate.
    -- @treturn Vector The negated vector.
    -- @usage -v
    unm = function(self)
        return vector.new(
            -self.x,
            -self.y,
            -self.z
        )
    end,

    --- Compute the dot product of two vectors
    --
    -- @tparam Vector self The first vector to compute the dot product of.
    -- @tparam Vector o The second vector to compute the dot product of.
    -- @treturn Vector The dot product of `self` and `o`.
    -- @usage v1:dot(v2)
    dot = function(self, o)
        return self.x * o.x + self.y * o.y + self.z * o.z
    end,

    --- Compute the cross product of two vectors
    --
    -- @tparam Vector self The first vector to compute the cross product of.
    -- @tparam Vector o The second vector to compute the cross product of.
    -- @treturn Vector The cross product of `self` and `o`.
    -- @usage v1:cross(v2)
    cross = function(self, o)
        return vector.new(
            self.y * o.z - self.z * o.y,
            self.z * o.x - self.x * o.z,
            self.x * o.y - self.y * o.x
        )
    end,

    --- Get the length (also referred to as magnitude) of this vector.
    -- @tparam Vector self This vector.
    -- @treturn number The length of this vector.
    length = function(self)
        return math.sqrt(self.x * self.x + self.y * self.y + self.z * self.z)
    end,

    --- Divide this vector by its length, producing with the same direction, but
    -- of length 1.
    --
    -- @tparam Vector self The vector to normalise
    -- @treturn Vector The normalised vector
    -- @usage v:normalize()
    normalize = function(self)
        return self:mul(1 / self:length())
    end,

    --- Construct a vector with each dimension rounded to the nearest value.
    --
    -- @tparam Vector self The vector to round
    -- @tparam[opt] number tolerance The tolerance that we should round to,
    -- defaulting to 1. For instance, a tolerance of 0.5 will round to the
    -- nearest 0.5.
    -- @treturn Vector The rounded vector.
    round = function(self, tolerance)
        tolerance = tolerance or 1.0
        return vector.new(
            math.floor((self.x + tolerance * 0.5) / tolerance) * tolerance,
            math.floor((self.y + tolerance * 0.5) / tolerance) * tolerance,
            math.floor((self.z + tolerance * 0.5) / tolerance) * tolerance
        )
    end,

    --- Convert this vector into a string, for pretty printing.
    --
    -- @tparam Vector self This vector.
    -- @treturn string This vector's string representation.
    -- @usage v:tostring()
    -- @usage tostring(v)
    tostring = function(self)
        return self.x .. "," .. self.y .. "," .. self.z
    end,

    --- Check for equality between two vectors.
    --
    -- @tparam Vector self The first vector to compare.
    -- @tparam Vector other The second vector to compare to.
    -- @treturn boolean Whether or not the vectors are equal.
    equals = function(self, other)
        return self.x == other.x and self.y == other.y and self.z == other.z
    end,
}

local vmetatable = {
    __index = vector,
    __add = vector.add,
    __sub = vector.sub,
    __mul = vector.mul,
    __div = vector.div,
    __unm = vector.unm,
    __tostring = vector.tostring,
    __eq = vector.equals,
}

--- Construct a new @{Vector} with the given coordinates.
--
-- @tparam number x The X coordinate or direction of the vector.
-- @tparam number y The Y coordinate or direction of the vector.
-- @tparam number z The Z coordinate or direction of the vector.
-- @treturn Vector The constructed vector.
function new(x, y, z)
    return setmetatable({
        x = tonumber(x) or 0,
        y = tonumber(y) or 0,
        z = tonumber(z) or 0,
    }, vmetatable)
end
