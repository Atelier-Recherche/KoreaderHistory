local WidgetContainer = require("ui/widget/container/widgetcontainer")
local DataStorage = require("datastorage")
local UIManager = require("ui/uimanager")
local logger = require("logger")
local lfs = require("libs/libkoreader-lfs")
local _ = require("gettext")

local AtHistory = WidgetContainer:extend{
    name = "athistory",
    is_doc_only = false,
}

local function pendingPath()
    return DataStorage:getSettingsDir() .. "/athistory/pending.lua"
end

local function ensureDir()
    local dir = DataStorage:getSettingsDir() .. "/athistory"
    if lfs.attributes(dir, "mode") ~= "directory" then
        lfs.mkdir(dir)
    end
end

local function readPending()
    local path = pendingPath()
    if lfs.attributes(path, "mode") ~= "file" then
        return nil
    end
    local ok, data = pcall(dofile, path)
    if not ok or type(data) ~= "table" then
        logger.warn("athistory: invalid pending.lua", data)
        return nil
    end
    return data, path
end

local function clearPending(path)
    if path then
        pcall(os.remove, path)
    end
end

local function getUI()
    local FileManager = require("apps/filemanager/filemanager")
    if FileManager.instance then
        return FileManager.instance
    end
    local ReaderUI = require("apps/reader/readerui")
    if ReaderUI.instance then
        return ReaderUI.instance
    end
    return nil
end

local function runDispatcher(action_id)
    if not action_id or action_id == "" then
        return false
    end
    local ok, Dispatcher = pcall(require, "dispatcher")
    if not ok or not Dispatcher then
        logger.warn("athistory: Dispatcher unavailable")
        return false
    end
    local action = {}
    action[action_id] = true
    local ok_exec = pcall(function()
        Dispatcher:execute(action)
    end)
    return ok_exec
end

local function openHistory()
    local ui = getUI()
    if ui and ui.history and ui.history.onShowHist then
        ui.history:onShowHist()
        return
    end
    local FileManagerHistory = require("apps/filemanager/filemanagerhistory")
    if ui then
        local hist = FileManagerHistory:new{ ui = ui }
        hist:onShowHist()
    end
end

local function openFolder(path)
    if not path or path == "" then
        return
    end
    local FileManager = require("apps/filemanager/filemanager")
    FileManager:showFiles(path)
end

local function openCollection(name)
    if not name or name == "" then
        return
    end
    local ui = getUI()
    if ui and ui.collections then
        if ui.collections.onShowCollList then
            pcall(function() ui.collections:onShowCollList() end)
        elseif ui.collections.onShowColl then
            pcall(function() ui.collections:onShowColl(name) end)
        end
        return
    end
    runDispatcher("show_collections")
end

local function continueReading()
    local ReadHistory = require("readhistory")
    if not ReadHistory.hist or #ReadHistory.hist == 0 then
        pcall(function() ReadHistory:reload() end)
    end
    local entry = ReadHistory.hist and ReadHistory.hist[1]
    if not entry or not entry.file then
        return
    end
    local ReaderUI = require("apps/reader/readerui")
    ReaderUI:showReader(entry.file)
end

local function getPluginInstance(plugin_name)
    local ui = getUI()
    if ui and type(ui[plugin_name]) == "table" then
        return ui[plugin_name]
    end
    local ok, PluginLoader = pcall(require, "pluginloader")
    if ok and PluginLoader and PluginLoader.getPluginInstance then
        local inst = PluginLoader:getPluginInstance(plugin_name)
        if inst then
            return inst
        end
    end
    return nil
end

local function invokeMenuCallback(inst, plugin_name)
    if type(inst.addToMainMenu) ~= "function" then
        return false
    end
    local probe = {}
    local ok = pcall(function() inst:addToMainMenu(probe) end)
    if not ok then
        return false
    end
    local preferred = {
        probe[plugin_name],
        probe[inst.name],
    }
    for _, entry in ipairs(preferred) do
        if type(entry) == "table" and type(entry.callback) == "function" then
            local cok = pcall(entry.callback)
            if cok then
                return true
            end
        end
    end
    -- Any first callback in the menu table
    for _, entry in pairs(probe) do
        if type(entry) == "table" and type(entry.callback) == "function" then
            local cok = pcall(entry.callback)
            if cok then
                return true
            end
        end
    end
    return false
end

local function callMethod(inst, method)
    if not method or method == "" then
        return false
    end
    if type(inst[method]) == "function" then
        local ok = pcall(function() inst[method](inst) end)
        return ok
    end
    return false
end

local function openPlugin(plugin_name, method)
    if not plugin_name or plugin_name == "" then
        logger.warn("athistory: open_plugin missing name")
        return
    end

    local inst = getPluginInstance(plugin_name)
    if not inst then
        logger.warn("athistory: plugin instance not found:", plugin_name)
        -- Last resort: dispatcher ids
        if method and method ~= "" then
            runDispatcher(method)
        end
        runDispatcher(plugin_name)
        return
    end

    logger.info("athistory: opening plugin", plugin_name, tostring(method))

    -- 1) Explicit method from the Android catalog
    if callMethod(inst, method) then
        return
    end

    -- 2) Well-known entry points
    local fallbacks = {
        method,
        "openAccountList", -- rssreader
        "openRoot",        -- bluesky
        "onShowMainCategory",
        "onShowCalendarView",
        "onShow",
        "show",
        "on" .. plugin_name:sub(1, 1):upper() .. plugin_name:sub(2),
    }
    for _, m in ipairs(fallbacks) do
        if callMethod(inst, m) then
            return
        end
    end

    -- 3) Same as SimpleUI: invoke the main-menu callback registered by the plugin
    if invokeMenuCallback(inst, plugin_name) then
        return
    end

    logger.warn("athistory: could not launch plugin UI for", plugin_name)
end

function AtHistory:executePending()
    ensureDir()
    local data, path = readPending()
    if not data then
        return
    end
    local action = data.action
    local args = data.args or {}
    clearPending(path)

    logger.info("athistory: executing", tostring(action), tostring(args.param), tostring(args.param2))

    -- Slightly longer delay so FileManager/ReaderUI + plugins are ready after cold start
    UIManager:scheduleIn(0.6, function()
        if action == "history" then
            openHistory()
        elseif action == "open_folder" then
            openFolder(args.param)
        elseif action == "open_collection" then
            openCollection(args.param or args.collection)
        elseif action == "continue" then
            continueReading()
        elseif action == "open_book" then
            local book = args.param
            if book and book ~= "" then
                local ReaderUI = require("apps/reader/readerui")
                ReaderUI:showReader(book)
            end
        elseif action == "dispatcher" then
            runDispatcher(args.param)
        elseif action == "open_plugin" then
            openPlugin(args.param, args.param2)
        else
            logger.warn("athistory: unknown action", tostring(action))
        end
    end)
end

function AtHistory:onDispatcherRegisterActions()
    local Dispatcher = require("dispatcher")
    Dispatcher:registerAction("athistory_poll", {
        category = "none",
        event = "AtHistoryPoll",
        title = _("Atelier History: poll pending"),
        general = true,
    })
end

function AtHistory:onAtHistoryPoll()
    self:executePending()
    return true
end

function AtHistory:init()
    self:onDispatcherRegisterActions()
    ensureDir()
    UIManager:scheduleIn(0.5, function()
        self:executePending()
    end)
    UIManager:scheduleIn(1.8, function()
        self:executePending()
    end)
end

function AtHistory:onResume()
    UIManager:scheduleIn(0.4, function()
        self:executePending()
    end)
    return false
end

function AtHistory:onReaderReady()
    UIManager:scheduleIn(0.4, function()
        self:executePending()
    end)
    return false
end

return AtHistory
