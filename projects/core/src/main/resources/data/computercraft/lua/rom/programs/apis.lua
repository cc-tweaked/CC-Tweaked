-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local tApis = {}
for k, v in pairs(_G) do
    if type(k) == "string" and type(v) == "table" and k ~= "_G" then
        table.insert(tApis, k)
    end
end
table.insert(tApis, "shell")
table.insert(tApis, "package")
if multishell then
    table.insert(tApis, "multishell")
end
table.sort(tApis)

textutils.pagedTabulate(tApis)
