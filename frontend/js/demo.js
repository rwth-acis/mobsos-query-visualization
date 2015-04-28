/**
 * Demo Application Script
 * @author Stephan Erdtmann (erdtmann@dbis.rwth-aachen.de)
 */

    //Forms and Sections
var loginFormNode              = document.getElementById("qv_loginform");
var queryFormNode              = document.getElementById("qv_queryform");
var previewSectionNode         = document.getElementById("qv_chart");
var addDatabaseFormNode        = document.getElementById("qv_add_database_form");
var removeDatabaseFormNode     = document.getElementById("qv_remove_database_form");
var addFilterFormNode          = document.getElementById("qv_add_filter_form");
var removeFilterFormNode       = document.getElementById("qv_remove_filter_form");
var removeQueryFormNode        = document.getElementById("qv_remove_query_form");

    //Login Form Nodes
var usernameNode               = document.getElementById("qv_username");
var passwordNode               = document.getElementById("qv_password");

    //Query Form Nodes
var selectQuery                = document.getElementById("qv_select_query");
var databaseNode               = document.getElementById("qv_database");
var queryNode                  = document.getElementById("qv_query");
var filterNode                 = document.getElementById("qv_filter");
var visualizationTypeNode      = document.getElementById("qv_visualization_type");
var modificationTypeNode       = document.getElementById("qv_modification_type");
var chartTitleNode             = document.getElementById("qv_chart_title");
var widthNode                  = document.getElementById("qv_width");
var heightNode                 = document.getElementById("qv_height");
var cacheNode                  = document.getElementById("qv_cache");
var saveButton                 = document.getElementById("qv_save_button");

    //Preview Section Nodes
var previewNode                = document.getElementById("qv_preview");
var generatedHtmlNode          = document.getElementById("qv_generated_html");
var generatedHtmlWrapperNode   = document.getElementById("qv_generated_html_wrapper");
var generatedWidgetNode        = document.getElementById("qv_generated_widget");
var generatedWidgetWrapperNode = document.getElementById("qv_generated_widget_wrapper");
var exportHTMLNode             = document.getElementById("qv_export_html");
var exportHTMLWrapperNode      = document.getElementById("qv_export_html_wrapper");
var exportCSVNode              = document.getElementById("qv_export_csv");
var exportCSVWrapperNode       = document.getElementById("qv_export_csv_wrapper");
var exportXMLNode              = document.getElementById("qv_export_xml");
var exportXMLWrapperNode       = document.getElementById("qv_export_xml_wrapper");
var exportJSONNode             = document.getElementById("qv_export_json");
var exportJSONWrapperNode      = document.getElementById("qv_export_json_wrapper");
var saveHtmlLinkNode           = document.getElementById("qv_save_html_link");
var saveWidgetLinkNode         = document.getElementById("qv_save_widget_link");
var saveExportHTMLLinkNode     = document.getElementById("qv_save_export_html_link");
var saveExportCSVLinkNode      = document.getElementById("qv_save_export_csv_link");
var saveExportXMLLinkNode      = document.getElementById("qv_save_export_xml_link");
var saveExportJSONLinkNode     = document.getElementById("qv_save_export_json_link");

    //Add Database Form Nodes
var addDatabaseDatabaseKeyNode = document.getElementById("qv_add_database_database_key");
var addDatabaseTypeCodeNode    = document.getElementById("qv_add_database_type_code");
var addDatabaseUsernameNode    = document.getElementById("qv_add_database_username");
var addDatabasePasswordNode    = document.getElementById("qv_add_database_password");
var addDatabaseDatabaseNode    = document.getElementById("qv_add_database");
var addDatabaseHostNode        = document.getElementById("qv_add_database_host");
var addDatabasePortNode        = document.getElementById("qv_add_database_port");

    //Remove Database Form Nodes
var removeDatabaseDatabaseNode = document.getElementById("qv_remove_database");

    //Add Filter Form Nodes
var addFilterFilterKeyNode     = document.getElementById("qv_add_filter_filter_key");
var addFilterDatabaseNode      = document.getElementById("qv_add_filter_database");
var addFilterQueryNode         = document.getElementById("qv_add_filter_query");

    //Remove Filter Form Nodes
var removeFilterFilterNode     = document.getElementById("qv_remove_filter");
var removeQueryQueryNode     = document.getElementById("qv_remove_query");

var demo = new QV.Visualizer();

/**
 * Stores if the tabs should be locked due to an query error
 * @type {Boolean}
 */
var locked = true;

/**
 * Stores which export data already has been loaded from backend.
 * @type {Array} Array with supposed indices 'preview', 'html', 'export_html', 'export_csv', 'export_xml', 'export_json'
 */
var ready = [];

/**
 * Stores the form data entered into the Query Form.
 * @type {Array} Array with supposed indices 'query', 'databaseKey', 'modificationTypeIndex', 'visualizationTypeIndex'
 */
var form_data = [];

/**
 * Stores the keys of the available filters
 * @type {Array} Array with filter keys
 */
var filterKeys = [];

var getRandomId = function(length,startWithLetter) {
    var text = "";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    if(typeof startWithLetter == "undefined") startWithLetter = false;

    if(startWithLetter) {
        text += possible.charAt(Math.floor(Math.random() * 52));
        length--;
    }

    for( var i=0; i < length; i++ )
    text += possible.charAt(Math.floor(Math.random() * possible.length));

    return text;
};

/**
 * Generates HTML and Widget XML Code
 * @param key The key of the visualization
 * @return {Object} Property html of the returned object contains the HTML code, property widget the Widget XML
 */
var getGeneratedCode = function(data){
    var l = window.location;
    data.sources = l.protocol + "//" + l.hostname + l.port + l.pathname.split("demo.html")[0];
    data.host = demo.getHost();
    data.path = demo.getPath();
    if (!data.eleId) {
        data.eleId = getRandomId(10, true);
    }
    if (data.filters) {
        if (!data.filterId) {
            data.filterId = getRandomId(10, true);
        }
        return {html: ich.qv_code_template_filters(data,true), widget: ich.qv_widget_template(data,true)};
    }
    return {html: ich.qv_code_template(data,true), widget: ich.qv_widget_template(data,true)};
};

/**
 * Retrieves the database keys from backend and fills the corresponding select form fields in Query Form and Remove Database Form with the data
 */
var load_database_keys = function(){
    demo.retrieveDatabaseKeys(function(keys){
        var i,
            numOfKeys = keys.length,
            data;
        $(databaseNode).empty();
        $(removeDatabaseDatabaseNode).empty();
        $(addFilterDatabaseNode).empty();
        for(i=0; i<numOfKeys; i++){
            data = {key: keys[i][0]};
            $(databaseNode).append(ich.qv_database_key_option_template(data));
            $(removeDatabaseDatabaseNode).append(ich.qv_database_key_option_template(data));
            $(addFilterDatabaseNode).append(ich.qv_database_key_option_template(data));
        }
    });
};

/**
 * Retrieves the filter keys from backend and fills the corresponding select form fields in Remove Filter Form with the data
 */
var load_filter_keys = function(){
    demo.retrieveFilterKeys(function(keys){
        var i,
            numOfKeys = keys.length,
            data;
        filterKeys = [];
        $(removeFilterFilterNode).empty();
        for(i=0; i<numOfKeys; i++){
            data = {db: keys[i][1], key: keys[i][0]};
            filterKeys.push(data);
            $(removeFilterFilterNode).append(ich.qv_filter_key_option_template(data));
        }
        load_filter_values(filterKeys);
    });
};

/**
 * Retrieves the filter values from backend and fills the corresponding select form fields in Query Form with the data
 */
var load_filter_values = function(keys){
    var i,
        j,
        numOfKeys = keys.length,
        numOfFilterKeys,
        data;

    $(filterNode).empty();

    if(numOfKeys === 0){
        $(filterNode).append("<p>No filters configured!</p>");
    }

    for(i=0; i<numOfKeys;i++){
        data = {db: keys[i].db, key: keys[i].key, key_lc: keys[i].key.toLowerCase(), name: keys[i].key, values: []};
        demo.retrieveFilterValues(keys[i].db, keys[i].key, (function(data){
            return function(filterKeys){
                    for(j=0, numOfFilterKeys = filterKeys.length; j<numOfFilterKeys; j++){
                        data.values.push(filterKeys[j][0]);
                    }
                    $(filterNode).append(ich.qv_filter_template(data));
            };
        })(data));
    }
};

/**
 * Retrieves the query keys from backend and fills the corresponding select form fields in Remove Query Form with the data
 */
var load_query_keys = function(){
    demo.retrieveQueryKeys(function(keys){
        var i,
            numOfKeys = keys.length,
            data;
        queryKeys = [];
        $(removeQueryQueryNode).empty();
        queries_locked = true;
        $(selectQuery).empty();
        for(i=0; i<numOfKeys; i++){
            data = {key: keys[i][0]};
            if (keys[i][2] !== "") {
                data.name = keys[i][2]+" ["+keys[i][1]+"]";
            } else {
                data.name = keys[i][0]+" ["+keys[i][1]+"]";
            }
            queryKeys.push(data);
            $(removeQueryQueryNode).append(ich.qv_query_key_option_template(data));
            $(selectQuery).append(ich.qv_query_key_option_template(data));
        }
        load_query_values(queryKeys);
        queries_locked = false;
    });
};

var query_cache = {};

/**
 * Retrieves the query values from backend and fills the corresponding select form fields in Query Form with the data
 */
var load_query_values = function(keys){
    var i,
        j,
        numOfKeys = keys.length,
        numOfQueryKeys,
        data;

    for(i=0; i<numOfKeys;i++){
        if (query_cache[keys[i].key]) {
            continue;
        }
        data = {key: keys[i].key, key_lc: keys[i].key.toLowerCase(), name: keys[i].name, values: []};
        demo.retrieveQueryValues(keys[i].key,(function(data){
            return function(query){
                query_cache[query.key] = query;
            };
        })(data));
    }
};

var fill_query_values = function(query) {
    for (var i = 0, len = databaseNode.options.length; i < len; i++) {
        if (databaseNode.options[i].text === query.db) {
            databaseNode.selectedIndex = i;
            break;
        }
    }
    chartTitleNode.value = query.title;
    widthNode.value = query.width;
    heightNode.value = query.height;
    // cacheNode = query.cache;
    queryNode.value = query.query;
    for (i = 0, len = visualizationTypeNode.options.length; i < len; i++) {
        if (visualizationTypeNode.options[i].value.toLowerCase() === query.format.toLowerCase()) {
            visualizationTypeNode.selectedIndex = i;
            break;
        }
    }
    for (i = 0, len = modificationTypeNode.options.length; i < len; i++) {
        if (modificationTypeNode.options[i].value.toLowerCase() === (""+query.modtypei).toLowerCase()) {
            modificationTypeNode.selectedIndex = i;
            break;
        }
    }
};

/**
 * Callback for the logging in with openid
 */
var signinCallback = function(result){
    if(result === "success"){
        // authenticated

        // OpenID Connect user info
        console.log(oidc_userinfo);
        $("#uname").html(oidc_userinfo.name);
        $(".authenticated").removeClass("hidden");
        show_query_form();
        load_database_keys();
        load_filter_keys();
        load_query_keys();
        save_disable();
    } else {
        // anonymous
    }
};

/**
 * Callback for the submission of the Login Form. Tries to login to LAS and shows the Query Form on success
 */
var login_form_submit = function(){
    var username = usernameNode.value,
        password = passwordNode.value;
    if(username !== ""){
        demo.login(username,password,function(){
            show_query_form();
            load_database_keys();
            load_filter_keys();
        });
    }
};

/**
 * Callback for the submission of the Add Database Form.
 */
var add_database_form_submit = function(){
    demo.addDatabase(addDatabaseDatabaseKeyNode.value,addDatabaseTypeCodeNode.options[addDatabaseTypeCodeNode.selectedIndex].value,addDatabaseUsernameNode.value,addDatabasePasswordNode.value,addDatabaseDatabaseNode.value,addDatabaseHostNode.value,addDatabasePortNode.value,function(){
        load_database_keys();
        addDatabaseDatabaseKeyNode.value = "";
        addDatabaseTypeCodeNode.selectedIndex = 0;
        addDatabaseUsernameNode.value = "";
        addDatabasePasswordNode.value = "";
        addDatabaseDatabaseNode.value = "";
        addDatabaseHostNode.value = "";
        suggest_default_port();
    });
};

/**
 * Callback for the submission of the Remove Database Form.
 */
var remove_database_form_submit = function(){
    demo.removeDatabase(removeDatabaseDatabaseNode.options[removeDatabaseDatabaseNode.selectedIndex].value,function(){
        load_database_keys();
    });
};

/**
 * Callback for the submission of the Add Database Form.
 */
var add_filter_form_submit = function(){
    demo.addFilter(addFilterFilterKeyNode.value,addFilterQueryNode.value,addFilterDatabaseNode.options[addFilterDatabaseNode.selectedIndex].value,function(){
        load_filter_keys();
        addFilterFilterKeyNode.value = "";
        addFilterDatabaseNode.selectedIndex = 0;
        addFilterQueryNode.value = "";
    });
};

/**
 * Callback for the submission of the Remove Database Form.
 */
var remove_filter_form_submit = function(){
    demo.removeFilter(removeFilterFilterNode.options[removeFilterFilterNode.selectedIndex].value,function(){
        load_filter_keys();
    });
};

/**
 * Callback for the submission of the Remove Query Form.
 */
var remove_query_form_submit = function(){
    demo.removeQuery(removeQueryQueryNode.options[removeQueryQueryNode.selectedIndex].value,function(){
        delete query_cache[removeQueryQueryNode.options[removeQueryQueryNode.selectedIndex].value];
        load_query_keys();
    });
};

/**
 * Suggests an default port for the selected database type in the Add Database Form and writes it into the corresponding form field of this form
 */
var suggest_default_port = function(){
    var typeCode = addDatabaseTypeCodeNode.options[addDatabaseTypeCodeNode.selectedIndex].value;
    addDatabasePortNode.value = QV.DATABASETYPE.fromCode(typeCode).DEFAULTPORT;
};

/**
 * Shows the help text
 */
var show_help = function(){
    var helpwindow = window.open("help.html", "Query Visualizer - Help", "width=1200,height=640,resizable=yes");
    helpwindow.focus();
};

/**
 * Shows the Add Database and Remove Database Form and hides the other sections
 */
var show_database_management = function(){
    $(loginFormNode).hide();
    $(addDatabaseFormNode).show();
    $(removeDatabaseFormNode).show();
    $(removeQueryFormNode).hide();
    $(addFilterFormNode).hide();
    $(removeFilterFormNode).hide();
    $(queryFormNode).hide();
    $(previewSectionNode).hide();
};

/**
 * Shows the Add Query and Remove Query Form and hides the other sections
 */
var show_query_management = function(){
    $(loginFormNode).hide();
    $(addDatabaseFormNode).hide();
    $(removeDatabaseFormNode).hide();
    $(removeQueryFormNode).show();
    $(addFilterFormNode).hide();
    $(removeFilterFormNode).hide();
    $(queryFormNode).hide();
    $(previewSectionNode).hide();
};

/**
 * Shows the Add Filter and Remove Filter Form and hides the other sections
 */
var show_filter_management = function(){
    $(loginFormNode).hide();
    $(removeQueryFormNode).hide();
    $(addDatabaseFormNode).hide();
    $(removeDatabaseFormNode).hide();
    $(addFilterFormNode).show();
    $(removeFilterFormNode).show();
    $(queryFormNode).hide();
    $(previewSectionNode).hide();
};

/**
 * Shows the Query Form and the Preview Section and hides the other sections
 */
var show_query_form = function(){
    $(loginFormNode).hide();
    $(addDatabaseFormNode).hide();
    $(removeDatabaseFormNode).hide();
    $(addFilterFormNode).hide();
    $(removeQueryFormNode).hide();
    $(removeFilterFormNode).hide();
    $(queryFormNode).show();
    $(previewSectionNode).show();
};

/**
 * Shows the Login Form and hides the other sections
 */
var show_login_form = function(){
    $(loginFormNode).show();
    $(addDatabaseFormNode).hide();
    $(removeDatabaseFormNode).hide();
    $(addFilterFormNode).hide();
    $(removeFilterFormNode).hide();
    $(removeQueryFormNode).hide();
    $(queryFormNode).hide();
    $(previewSectionNode).hide();
};

/**
 * Logs out and shows the Login Form and hides the other sections
 */
var logout = function(){
    demo.logout();
    show_login_form();
    lock_preview();
};

/**
 * Lock Tabs
 */
var lock_preview = function(){
    locked = true;
    $(previewSectionNode).addClass('locked');
};

/**
 * Unlock Tabs
 */
var unlock_preview = function(){
    locked = false;
    $(previewSectionNode).removeClass('locked');
};

var filter_regex = /\$([^\$]+)\$/g;

var get_available_filters = function() {
    var available_filters = {};
    for(i = 0; i<filterKeys.length; i++){
        available_filters[filterKeys[i].key] = $("#qv_filter_" + filterKeys[i].key.toLowerCase()).val();
    }
    return available_filters;
};

var filters_used = function(query) {
    if (!query) {
        query = queryNode.value;
    }
    var available_filters = get_available_filters();
    var found_filters = query.match(filter_regex);
    for (var i = 0, len = found_filters.length; i < len; i++) {
        found_filters[i] = found_filters[i].slice(1, -1);
    }
    var filters = {};
    if (found_filters !== null) {
        for (i = 0, len = found_filters.length; i < len; i++) {
            var found = found_filters[i];
            var filter = available_filters[found];
            if (filter) {
                filters[found] = filter;
            }
        }
    }
    return filters;
};

/**
 * Callback for the submission of the Query Form.
 */
var visualization_form_submit = function(){
    var i, numOfKeys = filterKeys.length, found;
    if(queryNode.value.trim() !== ""){
        toggle_tabs(1,1);
        toggle_tabs(2,1);

        form_data.query = queryNode.value;
        form_data.filters = get_available_filters();
        form_data.databaseKey = databaseNode.options[databaseNode.selectedIndex].value;
        form_data.modificationTypeIndex = parseInt(modificationTypeNode.options[modificationTypeNode.selectedIndex].value);
        form_data.visualizationTypeIndex = visualizationTypeNode.options[visualizationTypeNode.selectedIndex].value;
        form_data.title = chartTitleNode.value;
        form_data.width = widthNode.value;
        form_data.height = heightNode.value;
        form_data.cache = cacheNode.checked;

        form_data.visualizationOptions = [];
        form_data.visualizationOptions[0] = chartTitleNode.value;
        form_data.visualizationOptions[1] = widthNode.value;
        form_data.visualizationOptions[2] = heightNode.value;

        form_data.queryParams = [];

        form_data.query.replace(filter_regex,function($0,$1){
            found = false;
            for(i=0; i<numOfKeys; i++){
                if(filterKeys[i].key == $1){
                    found = true;
                }
            }
            if (found) form_data.queryParams.push(form_data.filters[$1]);
            return $0;
        });

        console.log(form_data);

        lock_preview();
        ready = [];
        load_preview();
    }
};

/**
 * Shows the Preview Tab, retrieves the visualization from backend and shows it in the corresponding node
 */
var load_preview = function(){
    toggle_tabs(1,1);
    toggle_tabs(2,1);
    if(ready.preview === undefined){
        $(previewNode).empty();
        $(previewNode).addClass("loading");
        demo.retrieve(form_data.query,form_data.queryParams,form_data.databaseKey,form_data.modificationTypeIndex,form_data.visualizationTypeIndex,form_data.visualizationOptions,previewNode,function(result){
            if(!/^The Query has lead to an error./.test(result)){
                unlock_preview();
            }
            ready.preview = true;
            $(previewNode).removeClass("loading");
            save_enable();
        });
    }
};

/**
 * Saves the current query
 */
var save_query = function(){
    demo.save(form_data.query,form_data.queryParams,form_data.databaseKey,form_data.modificationTypeIndex,form_data.visualizationTypeIndex,form_data.visualizationOptions,null,function(result){
        for (var prop in query_cache) {
            if (query_cache[prop].title == form_data.title) {
                delete query_cache[prop];
            }
        }
        alert("Query saved");
        load_query_keys();
        save_disable();
    });
};

/**
 * Retrieves the chart key from backend, generates the embed codes (HTML and Widget XML) and shows them in the corresponding nodes
 */
var load_embed_code = function(){
    if(ready.html === undefined){
        $(generatedHtmlWrapperNode).children().hide();
        $(generatedHtmlWrapperNode).addClass("loading");
        $(generatedWidgetWrapperNode).children().hide();
        $(generatedWidgetWrapperNode).addClass("loading");
        demo.retrieveChartKey(form_data.query,form_data.queryParams,form_data.databaseKey,form_data.modificationTypeIndex,form_data.visualizationTypeIndex,form_data.visualizationOptions,form_data.cache,function(key){
            var generatedCode;
            var data = {key: key};
            data.filters = filters_used(form_data.query);
            generatedCode = getGeneratedCode(data);
            ready.html = true;
            generatedHtmlNode.value = generatedCode.html;
            saveHtmlLinkNode.href='data:text/plain;base64,' + btoa(generatedCode.html);
            generatedWidgetNode.value = generatedCode.widget;
            saveWidgetLinkNode.href='data:text/plain;base64,' + btoa(generatedCode.widget);
            $(generatedHtmlWrapperNode).children().show();
            $(generatedHtmlWrapperNode).removeClass("loading");
            $(generatedWidgetWrapperNode).children().show();
            $(generatedWidgetWrapperNode).removeClass("loading");
            for (var prop in query_cache) {
                if (query_cache[prop].title == form_data.title) {
                    delete query_cache[prop];
                }
            }
            load_query_keys();
        });
    }
};

/**
 * Shows the HTML-Code Tab
 */
var load_html = function(){
    if(!locked){
        toggle_tabs(1,1);
        toggle_tabs(2,2);
        load_embed_code();
    }
};

/**
 * Shows the Widget XML Tab
 */
var load_widget_xml = function(){
    if(!locked){
        toggle_tabs(1,1);
        toggle_tabs(2,3);
        load_embed_code();
    }
};

/**
 * Shows the Export As HTML-Table Tab, retrieves the data as HTML-Table from backend and shows it in the corresponding node
 */
var load_export_html = function(){
    if(!locked){
        toggle_tabs(1,2);
        toggle_tabs(3,1);
        if(ready.export_html === undefined){
            $(exportHTMLWrapperNode).children().hide();
            $(exportHTMLWrapperNode).addClass("loading");
            demo.retrieve(form_data.query,form_data.queryParams,form_data.databaseKey,form_data.modificationTypeIndex,QV.VISUALIZATIONTYPE.HTMLTABLE.STRING,form_data.visualizationOptions,null,function(data){
                ready.export_html = true;
                exportHTMLNode.value = data.replace(/(\r\n|\r|\n)/g, '\r\n').replace(/\r\n$/,"");
                saveExportHTMLLinkNode.href='data:text/plain;base64,' + btoa(data);
                $(exportHTMLWrapperNode).children().show();
                $(exportHTMLWrapperNode).removeClass("loading");
            });
        }
    }
};

/**
 * Shows the Export As CSV Tab, retrieves the data as CSV from backend and shows it in the corresponding node
 */
var load_export_csv = function(){
    if(!locked){
        toggle_tabs(1,2);
        toggle_tabs(3,2);
        if(ready.export_csv === undefined){
            $(exportCSVWrapperNode).children().hide();
            $(exportCSVWrapperNode).addClass("loading");
            demo.retrieve(form_data.query,form_data.queryParams,form_data.databaseKey,form_data.modificationTypeIndex,QV.VISUALIZATIONTYPE.CSV.STRING,form_data.visualizationOptions,null,function(data){
                ready.export_csv = true;
                exportCSVNode.value = data.replace(/(\r\n|\r|\n)/g, '\r\n').replace(/\r\n$/,"");
                saveExportCSVLinkNode.href='data:text/plain;base64,' + btoa(data);
                $(exportCSVWrapperNode).children().show();
                $(exportCSVWrapperNode).removeClass("loading");
            });
        }
    }
};

/**
 * Shows the Export As XML Tab, retrieves the data as XML from backend and shows it in the corresponding node
 */
var load_export_xml = function(){
    if(!locked){
        toggle_tabs(1,2);
        toggle_tabs(3,3);
        if(ready.export_xml === undefined){
            $(exportXMLWrapperNode).children().hide();
            $(exportXMLWrapperNode).addClass("loading");
            demo.retrieve(form_data.query,form_data.queryParams,form_data.databaseKey,form_data.modificationTypeIndex,QV.VISUALIZATIONTYPE.XML.STRING,form_data.visualizationOptions,null,function(data){
                ready.export_xml = true;
                var serialized = new XMLSerializer().serializeToString(data);
                exportXMLNode.value = serialized.replace(/(\r\n|\r|\n)/g, '\r\n').replace(/\r\n$/,"");
                saveExportXMLLinkNode.href='data:text/plain;base64,' + btoa(serialized);
                $(exportXMLWrapperNode).children().show();
                $(exportXMLWrapperNode).removeClass("loading");
            });
        }
    }
};

/**
 * Shows the Export As JSON Tab, retrieves the data as JSON from backend and shows it in the corresponding node
 */
var load_export_json = function(){
    if(!locked){
        toggle_tabs(1,2);
        toggle_tabs(3,4);
        if(ready.export_json === undefined){
            $(exportJSONWrapperNode).children().hide();
            $(exportJSONWrapperNode).addClass("loading");
            demo.retrieve(form_data.query,form_data.queryParams,form_data.databaseKey,form_data.modificationTypeIndex,QV.VISUALIZATIONTYPE.JSON.STRING,form_data.visualizationOptions,null,function(data){
                ready.export_json = true;
                var serialized = JSON.stringify(data);
                exportJSONNode.value = serialized.replace(/(\r\n|\r|\n)/g, '\r\n').replace(/\r\n$/,"");
                saveExportJSONLinkNode.href='data:text/plain;base64,' + btoa(serialized);
                $(exportJSONWrapperNode).children().show();
                $(exportJSONWrapperNode).removeClass("loading");
            });
        }
    }
};

/**
 * Show the Tab with id tabId form Tabset with id tabsetId
 * @param tabsetId Id of the Tabset
 * @param tabId Id of the tab
 */
var toggle_tabs = function(tabsetId,tabId){
    $("#tabset"+tabsetId+" li").removeClass("active");
    $("#tabset"+tabsetId+">li:nth-child("+tabId+")").addClass("active");
    $(".tabs"+tabsetId).hide();
    $(".tabs"+tabsetId+":eq("+(tabId-1)+")").show();
};

/**
 * Show / Hide the metadata input fields
 */
var toggle_metadata = function(){
    $('#qv_metadata').toggle();
};

/**
 * Show / Hide the filter input fields
 */
var toggle_filter = function(){
    $('#qv_filter').toggle();
};

//Show Login Form by default
show_login_form();

//Initialize the Tabsets
toggle_tabs(3,1);
toggle_tabs(2,1);
toggle_tabs(1,1);

//Lock Tabs
lock_preview();

//Hide metadata fields
toggle_metadata();

//Hide filter fields
toggle_filter();

var queries_locked = false;

$(selectQuery).change(function(){
    if (!queries_locked) {
        var selected_key = $(selectQuery).find("option:selected").val();
        var query = query_cache[selected_key];
        fill_query_values(query);
    }
});

var save_disable = function() {
    var sb = $(saveButton);
    sb.prop("disabled", true);
};

var save_enable = function() {
    var sb = $(saveButton);
    sb.removeProp("disabled");
};

$(queryFormNode).find("textarea,input,select").change(save_disable);
$(saveButton).click(save_query);
