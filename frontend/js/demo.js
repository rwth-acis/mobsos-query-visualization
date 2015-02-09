/**
 * Demo Application Script
 * @author Stephan Erdtmann (erdtmann@dbis.rwth-aachen.de)
 */

    //Forms and Sections
var loginFormNode              = document.getElementById("qv_loginform"),
    queryFormNode              = document.getElementById("qv_queryform"),
    previewSectionNode         = document.getElementById("qv_chart"),
    addDatabaseFormNode        = document.getElementById("qv_add_database_form"),
    removeDatabaseFormNode     = document.getElementById("qv_remove_database_form"),
    addFilterFormNode          = document.getElementById("qv_add_filter_form"),
    removeFilterFormNode       = document.getElementById("qv_remove_filter_form"),

    //Login Form Nodes
    usernameNode               = document.getElementById("qv_username"),
    passwordNode               = document.getElementById("qv_password"),

    //Query Form Nodes
    databaseNode               = document.getElementById("qv_database"),
    queryNode                  = document.getElementById("qv_query"),
    filterNode                 = document.getElementById("qv_filter"),
    visualizationTypeNode      = document.getElementById("qv_visualization_type"),
    modificationTypeNode       = document.getElementById("qv_modification_type"),
    chartTitleNode             = document.getElementById("qv_chart_title"),
    widthNode                  = document.getElementById("qv_width"),
    heightNode                 = document.getElementById("qv_height"),
    cacheNode                  = document.getElementById("qv_cache"),

    //Preview Section Nodes
    previewNode                = document.getElementById("qv_preview"),
    generatedHtmlNode          = document.getElementById("qv_generated_html"),
    generatedHtmlWrapperNode   = document.getElementById("qv_generated_html_wrapper"),
    generatedWidgetNode        = document.getElementById("qv_generated_widget"),
    generatedWidgetWrapperNode = document.getElementById("qv_generated_widget_wrapper"),
    exportHTMLNode             = document.getElementById("qv_export_html"),
    exportHTMLWrapperNode      = document.getElementById("qv_export_html_wrapper"),
    exportCSVNode              = document.getElementById("qv_export_csv"),
    exportCSVWrapperNode       = document.getElementById("qv_export_csv_wrapper"),
    exportXMLNode              = document.getElementById("qv_export_xml"),
    exportXMLWrapperNode       = document.getElementById("qv_export_xml_wrapper"),
    exportJSONNode             = document.getElementById("qv_export_json"),
    exportJSONWrapperNode      = document.getElementById("qv_export_json_wrapper"),
    saveHtmlLinkNode           = document.getElementById("qv_save_html_link"),
    saveWidgetLinkNode         = document.getElementById("qv_save_widget_link"),
    saveExportHTMLLinkNode     = document.getElementById("qv_save_export_html_link"),
    saveExportCSVLinkNode      = document.getElementById("qv_save_export_csv_link"),
    saveExportXMLLinkNode      = document.getElementById("qv_save_export_xml_link"),
    saveExportJSONLinkNode     = document.getElementById("qv_save_export_json_link"),

    //Add Database Form Nodes
    addDatabaseDatabaseKeyNode = document.getElementById("qv_add_database_database_key"),
    addDatabaseTypeCodeNode    = document.getElementById("qv_add_database_type_code"),
    addDatabaseUsernameNode    = document.getElementById("qv_add_database_username"),
    addDatabasePasswordNode    = document.getElementById("qv_add_database_password"),
    addDatabaseDatabaseNode    = document.getElementById("qv_add_database"),
    addDatabaseHostNode        = document.getElementById("qv_add_database_host"),
    addDatabasePortNode        = document.getElementById("qv_add_database_port"),

    //Remove Database Form Nodes
    removeDatabaseDatabaseNode = document.getElementById("qv_remove_database");

    //Add Filter Form Nodes
    addFilterFilterKeyNode     = document.getElementById("qv_add_filter_filter_key"),
    addFilterDatabaseNode      = document.getElementById("qv_add_filter_database"),
    addFilterQueryNode         = document.getElementById("qv_add_filter_query"),

    //Remove Filter Form Nodes
    removeFilterFilterNode     = document.getElementById("qv_remove_filter");

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

/**
 * Generates HTML and Widget XML Code
 * @param key The key of the visualization
 * @return {Object} Property html of the returned object contains the HTML code, property widget the Widget XML
 */
var getGeneratedCode = function(key){
    var data = {key: key};
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
            data = {key: keys[i][0], name: keys[i][0]+" ["+keys[i][1]+"]"};
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

    if(numOfKeys == 0){
        $(filterNode).append("<p>No filters configured!</p>");
    }

    for(i=0; i<numOfKeys;i++){
        data = {key: keys[i].key, key_lc: keys[i].key.toLowerCase(), name: keys[i].name, values: []};
        demo.retrieveFilterValues(keys[i].key,(function(data){
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
 * Suggests an default port for the selected database type in the Add Database Form and writes it into the corresponding form field of this form
 */
var suggest_default_port = function(){
    var typeCode = parseInt(addDatabaseTypeCodeNode.options[addDatabaseTypeCodeNode.selectedIndex].value);
    addDatabasePortNode.value = QV.DATABASETYPE.fromInt(typeCode).DEFAULTPORT;
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

/**
 * Callback for the submission of the Query Form.
 */
var visualization_form_submit = function(){
    var i, numOfKeys = filterKeys.length, found;
    if(queryNode.value.trim() != ""){
        toggle_tabs(1,1);
        toggle_tabs(2,1);

        form_data['query'] = queryNode.value;
        form_data['filters'] = [];
        for(i = 0; i<numOfKeys; i++){
            form_data['filters'][filterKeys[i].key] = $("#qv_filter_" + filterKeys[i].key.toLowerCase()).val();
        }
        form_data['databaseKey'] = databaseNode.options[databaseNode.selectedIndex].value;
        form_data['modificationTypeIndex'] = parseInt(modificationTypeNode.options[modificationTypeNode.selectedIndex].value);
        form_data['visualizationTypeIndex'] = parseInt(visualizationTypeNode.options[visualizationTypeNode.selectedIndex].value);
        form_data['title'] = chartTitleNode.value;
        form_data['width'] = widthNode.value;
        form_data['height'] = heightNode.value;
        form_data['cache'] = cacheNode.checked;

        form_data['visualizationOptions'] = [];
        form_data['visualizationOptions'][0] = chartTitleNode.value;
        form_data['visualizationOptions'][1] = widthNode.value;
        form_data['visualizationOptions'][2] = heightNode.value;

        form_data['queryParams'] = [];

        form_data['query'].replace(/\$([^\$]+)\$/g,function($0,$1){
            found = false;
            for(i=0; i<numOfKeys; i++){
                if(filterKeys[i].key == $1){
                    found = true;
                }
            }
            if (found) form_data['queryParams'].push(form_data['filters'][$1]);
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
    if(ready['preview'] == undefined){
        $(previewNode).empty();
        $(previewNode).addClass("loading");
        demo.retrieve(form_data['query'],form_data['queryParams'],form_data['databaseKey'],form_data['modificationTypeIndex'],form_data['visualizationTypeIndex'],form_data['visualizationOptions'],previewNode,function(result){
            if(!/^The Query has lead to an error./.test(result)){
                unlock_preview();
            }
            ready['preview'] = true;
            $(previewNode).removeClass("loading");
        });
    }
};

/**
 * Retrieves the chart key from backend, generates the embed codes (HTML and Widget XML) and shows them in the corresponding nodes
 */
var load_embed_code = function(){
    if(ready['html'] == undefined){
        $(generatedHtmlWrapperNode).children().hide();
        $(generatedHtmlWrapperNode).addClass("loading");
        $(generatedWidgetWrapperNode).children().hide();
        $(generatedWidgetWrapperNode).addClass("loading");
        demo.retrieveChartKey(form_data['query'],form_data['queryParams'],form_data['databaseKey'],form_data['modificationTypeIndex'],form_data['visualizationTypeIndex'],form_data['visualizationOptions'],form_data['cache'],function(data){
            var generatedCode;
            generatedCode = getGeneratedCode(data);
            ready['html'] = true;
            generatedHtmlNode.value = generatedCode.html;
            saveHtmlLinkNode.href='data:text/plain;base64,' + btoa(generatedCode.html);
            generatedWidgetNode.value = generatedCode.widget;
            saveWidgetLinkNode.href='data:text/plain;base64,' + btoa(generatedCode.widget);
            $(generatedHtmlWrapperNode).children().show();
            $(generatedHtmlWrapperNode).removeClass("loading");
            $(generatedWidgetWrapperNode).children().show();
            $(generatedWidgetWrapperNode).removeClass("loading");
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
        if(ready['export_html'] == undefined){
            $(exportHTMLWrapperNode).children().hide();
            $(exportHTMLWrapperNode).addClass("loading");
            demo.retrieve(form_data['query'],form_data['queryParams'],form_data['databaseKey'],form_data['modificationTypeIndex'],QV.VISUALIZATIONTYPE.HTMLTABLE,form_data['visualizationOptions'],null,function(data){
                ready['export_html'] = true;
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
        if(ready['export_csv'] == undefined){
            $(exportCSVWrapperNode).children().hide();
            $(exportCSVWrapperNode).addClass("loading");
            demo.retrieve(form_data['query'],form_data['queryParams'],form_data['databaseKey'],form_data['modificationTypeIndex'],QV.VISUALIZATIONTYPE.CSV,form_data['visualizationOptions'],null,function(data){
                ready['export_csv'] = true;
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
        if(ready['export_xml'] == undefined){
            $(exportXMLWrapperNode).children().hide();
            $(exportXMLWrapperNode).addClass("loading");
            demo.retrieve(form_data['query'],form_data['queryParams'],form_data['databaseKey'],form_data['modificationTypeIndex'],QV.VISUALIZATIONTYPE.XML,form_data['visualizationOptions'],null,function(data){
                ready['export_xml'] = true;
                exportXMLNode.value = data.replace(/(\r\n|\r|\n)/g, '\r\n').replace(/\r\n$/,"");
                saveExportXMLLinkNode.href='data:text/plain;base64,' + btoa(data);
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
        if(ready['export_json'] == undefined){
            $(exportJSONWrapperNode).children().hide();
            $(exportJSONWrapperNode).addClass("loading");
            demo.retrieve(form_data['query'],form_data['queryParams'],form_data['databaseKey'],form_data['modificationTypeIndex'],QV.VISUALIZATIONTYPE.JSON,form_data['visualizationOptions'],null,function(data){
                ready['export_json'] = true;
                exportJSONNode.value = data.replace(/(\r\n|\r|\n)/g, '\r\n').replace(/\r\n$/,"");
                saveExportJSONLinkNode.href='data:text/plain;base64,' + btoa(data);
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
}

/**
 * Show / Hide the filter input fields
 */
var toggle_filter = function(){
    $('#qv_filter').toggle();
}

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
