/**
 * Query Visualization JS Library
 * @author Stephan Erdtmann (erdtmann@dbis.rwth-aachen.de)
 */

var QV = (function(QV){

    /**
     * Container for some help functions
     * @type {Object}
     */
    QV.HELPER = {};

    /**
     * Clears all child nodes of a node
     * @param parent The node that should be emptied
     */
    QV.HELPER.clearChildNodes = function(parent){
        while(parent.childNodes.length >= 1) {
            parent.removeChild(parent.firstChild);
        }
    };

    /**
     * Checks if an objects has a particular method
     * @param obj The object to examine
     * @param methodname The name of the method
     * @return {Boolean} True iff method was found
     */
    QV.HELPER.hasMethod = function(obj,methodname){
        return (typeof obj[methodname] == 'function');
    };

    /**
     * Generates an alphanumerical random string
     * @param length The length of the random string
     * @param startWithLetter {Boolean} Sets if random string should start with a letter
     * @return {String} The generated random string
     */
    QV.HELPER.getRandomId = function(length,startWithLetter)
    {
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
     * Strips all script tags of an HTML snippet and executes them
     * @see http://stackoverflow.com/questions/2592092/executing-script-elements-inserted-with-innerhtml#2967069
     * @param text The HTML snippets
     * @return {String} The cleaned HTML snippet
     */
    QV.HELPER.stripAndExecuteScript = function(text){
        var scripts = '';
        var cleaned = text.replace(/<script[^>]*>([\s\S]*?)<\/script>/gi, function(){
            scripts += arguments[1];
            return '';
        });

        /* jshint ignore:start */
        if (window.execScript){
            window.execScript(scripts);
        } else {

            var head = document.getElementsByTagName('head')[0];
            var scriptElement = document.createElement('script');
            scriptElement.setAttribute('type', 'text/javascript');
            scriptElement.innerHTML = scripts;
            head.appendChild(scriptElement);
            head.removeChild(scriptElement);
        }
        /* jshint ignore:end */
        return cleaned;
    };

    /**
     * Enum of the supported Modification Types
     * @type {Object}
     */
    QV.MODIFICATIONTYPE = {
        IDENTITY: 0,
        LOGARITHMIC: 1,
        NORMALIZATION: 2,

        fromInt: function(i){
            for(var key in this){
                if(this.hasOwnProperty(key)){
                    if(typeof this[key] == "number" && this[key] == i){
                        return key;
                    }
                }
            }
            return "NO_SUCH_TYPE";
        }
    };

    /**
     * Enum of the supported Visualization Types
     * @type {Object}
     */
    QV.VISUALIZATIONTYPE = {
        CSV:                    {ID: 0, STRING: "csv"},
        JSON:                   {ID: 1, STRING: "JSON"},
        HTMLTABLE:              {ID: 2, STRING: "htmltable"},
        XML:                    {ID: 3, STRING: "xml"},
        GOOGLEPIECHART:         {ID: 4, STRING: "googlepiechart"},
        GOOGLEBARCHART:         {ID: 5, STRING: "googlebarchart"},
        GOOGLELINECHART:        {ID: 6, STRING: "googlelinechart"},
        GOOGLETIMELINECHART:    {ID: 7, STRING: "googletimelinechart"},
        GOOGLERADARCHART:       {ID: 8, STRING: "googleradarchart"},
        GOOGLETABLE:            {ID: 9, STRING: "googletable"},

        fromInt: function(i){
            for(var key in this){
                if(this.hasOwnProperty(key)){
                    if(typeof this[key] == "object" && typeof this[key].ID == 'number' && this[key].ID == i){
                        return this[key];
                    }
                }
            }
            return "NO_SUCH_TYPE";
        }
    };

    /**
     * Enum of the supported Database Types
     * @type {Object}
     */
    QV.DATABASETYPE = {
        DB2:        {ID: 1, DEFAULTPORT: 50000, STRING: "db2"},
        MYSQL:      {ID: 2, DEFAULTPORT: 3306, STRING: "mysql"},
        FIREBIRD:   {ID: 3, DEFAULTPORT: 3050, STRING: "firebird"},
        MSSQL:      {ID: 4, DEFAULTPORT: 1433, STRING: "mssql"},
        POSTGRESQL: {ID: 5, DEFAULTPORT: 5432, STRING: "postgresql"},
        DERBY:      {ID: 6, DEFAULTPORT: 1527, STRING: "derby"},
        ORACLE:     {ID: 7, DEFAULTPORT: 1521, STRING: "oracle"},

        fromInt: function(i){
            for(var key in this){
                if(this.hasOwnProperty(key)){
                    if(typeof this[key] == "object" && typeof this[key].ID == 'number' && this[key].ID == i){
                        return this[key];
                    }
                }
            }
            return "NO_SUCH_TYPE";
        },

        fromCode: function(i){
            for(var key in this){
                if(this.hasOwnProperty(key)){
                    if(typeof this[key] == "object" && typeof this[key].STRING == 'string' && this[key].STRING == i){
                        return this[key];
                    }
                }
            }
            return "NO_SUCH_TYPE";
        }
    };

    /**
     * The Query Visualization Library
     * @return {Object}
     */
    QV.Visualizer = function(){

        /* * *                  *
         *  private properties  *
         *                  * * */

        var LASHOST = window.LASHOST || "http://localhost:8080/";
        var QVSPATH = window.QVSPATH || "QVS/";
        var LASSERVICENAME = "i5.las2peer.services.queryVisualization.QueryVisualizationService";
        var LASUSERNAME = "anonymous";
        var LASPASSWORD = "anonymous";

        // create new instance of TemplateServiceClient, given its endpoint URL
        var restClient = new TemplateServiceClient(LASHOST + QVSPATH);

        var visualizations = {};
        var filters = {};
        var autoupdate = false;

        /* * *               *
         *  private methods  *
         *               * * */

        /**
         * Writes msg to console log / logging to console might be replaced or disabled
         * @param msg the log message
         */
        var log = function(msg){
            console.log(msg);
        };

        /**
         * Displays an Error message if a REST call fails
         * @param error the error message
         * @param status the HTTP status code
         */
        var error = function(error, status) {
            alert("Error! Message: " + error + " HTTP Code: " + status);
        };

        /**
         * Retrieves the visualization  Key for a query and a set of database and modification options
         * @param query The query string
         * @param queryParams Query Parameters
         * @param databaseKey The identifier of the database to query
         * @param modficationTypeIndex The index of the modification function to apply
         * @param visualizationTypeIndex The index of the visualization type the result should be visualized with
         * @param visualizationOptions An array of
         * @param useCache Use cache for query
         * @param outputNode The DOM node to embed the visualization into
         * @param callback Callback function, called when the result has been retrieved. Has one paramter consisting of the retrieved data (visualization's HTML code or key)
         */
        var save = function(query,queryParams,databaseKey,modficationTypeIndex,visualizationTypeIndex,visualizationOptions,useCache,outputNode,callback){
            if(!restClient.loggedIn()) return;

            var success = function(result, type, status) {
                var output;
                if(outputNode){
                    output = result;
                    if(/^The Query has lead to an error./.test(output)){
                        output = "<p>The Query has lead to an error. See console for more info.</p>";
                        console.log(result);
                    }
                    outputNode.innerHTML = QV.HELPER.stripAndExecuteScript(" " + output);
                }
                if(typeof callback == 'function'){
                    if (result === null) {
                        restClient.post("query", content, qParams, success, error);
                    } else {
                        callback(result);
                    }
                }
            };

            var content = {
                query: query,
                queryparams: queryParams,
                dbkey: databaseKey,
                cache: useCache,
                modtypei: modficationTypeIndex,
                title: visualizationOptions[0],
                width: visualizationOptions[1],
                height: visualizationOptions[2],
            };

            var qParams = {
                format: visualizationTypeIndex
            };

            restClient.post("query", content, qParams, success, error);
        };

        /**
         * Retrieves the visualization resp. the visualization Key for a query and a set of database and modification options
         * @param query The query string
         * @param queryParams Query Parameters
         * @param databaseKey The identifier of the database to query
         * @param modficationTypeIndex The index of the modification function to apply
         * @param visualizationTypeIndex The index of the visualization type the result should be visualized with
         * @param visualizationOptions An array of
         * @param useCache Use cache for query
         * @param outputNode The DOM node to embed the visualization into
         * @param callback Callback function, called when the result has been retrieved. Has one paramter consisting of the retrieved data (visualization's HTML code or key)
         */
        var retrieve = function(query,queryParams,databaseKey,modficationTypeIndex,visualizationTypeIndex,visualizationOptions,useCache,outputNode,callback){
            if(!restClient.loggedIn()) return;

            var success = function(result, type, status) {
                var output;
                if(outputNode){
                    output = result;
                    if(/^The Query has lead to an error./.test(output)){
                        output = "<p>The Query has lead to an error. See console for more info.</p>";
                        console.log(result);
                    }
                    outputNode.innerHTML = QV.HELPER.stripAndExecuteScript(" " + output);
                }
                if(typeof callback == 'function'){
                    if (result === null) {
                        restClient.post("query", content, qParams, success, error);
                    } else {
                        callback(result);
                    }
                }
            };

            var content = {
                query: query,
                queryparams: queryParams,
                dbkey: databaseKey,
                cache: useCache,
                modtypei: modficationTypeIndex,
                title: visualizationOptions[0],
                width: visualizationOptions[1],
                height: visualizationOptions[2],
            };

            var qParams = {
                format: visualizationTypeIndex
            };

            restClient.post("query/visualize", content, qParams, success, error);
        };

        /**
         * Retrieves the visualization for a query key
         * @param key The query key
         * @param outputnode The DOM node to embed the visualization into
         * @param callback Callback function, called when the result has been retrieved. Has one paramter consisting of the retrieved data (visualization's HTML code)
         */
        var retrieveFromKey = function(key,outputnode,callback,filterValues){
            // if(!restClient.loggedIn()) return;
            var queryURL = "query/" + key + "/visualize";

            var content = null;
            if (filterValues) {
                content = {queryparams: filterValues};
            }

            var success = function(result, type, status) {
                outputnode.innerHTML = QV.HELPER.stripAndExecuteScript(" " + result);
                if(typeof callback == 'function'){
                    if (result === null) {
                        if(filterValues) {
                            restClient.post(queryURL, content, null, success, error);
                        } else {
                            restClient.get(queryURL, content, null, success, error);
                        }
                    } else {
                        callback(result);
                    }
                }
            };

            if(filterValues) {
                restClient.post(queryURL, content, null, success, error);
            } else {
                restClient.get(queryURL, content, null, success, error);
            }
        };

        /* * *              *
         *  public methods  *
         *              * * */

        return {
            addVisualization: function(key, callback){
                visualizations[key] = {key: key, cb: callback};
            },

            removeVisualization: function(key) {
                delete visualizations[key];
            },

            trigger: function(event) {
                for (var key in visualizations) {
                    if (typeof visualizations[key].cb === "function") {
                        visualizations[key].cb(event);
                        console.log("Triggered vis " + key + " with event ", event);
                    }
                }
            },

            /**
             * Adds a new database to the set of the configured databases of the user currently logged in
             * @param databaseKey
             * @param databaseTypeCode
             * @param username
             * @param password
             * @param database
             * @param host
             * @param port
             * @param callback Callback, called when the database has been added successfully. Has one paramter consisting of the database key of the added database
             */
            addDatabase: function(databaseKey,databaseTypeCode,username,password,database,host,port,callback){
                if(!restClient.loggedIn()) return;

                var content = {
                    db_code: databaseTypeCode,
                    username: username,
                    password: password,
                    database: database,
                    dbhost: host,
                    port: parseInt(port),
                };
                var queryParams = {
                    format: QV.VISUALIZATIONTYPE.JSON.STRING
                };

                var success = function(result, type, status) {
                    if (typeof result !== "object") {
                        try{
                            result = JSON.parse(result);
                        } catch (e){
                            alert("Error! Failed to parse JSON");
                            return;
                        }
                    }
                    if(result[2] == databaseKey){
                        alert("Database added!");
                        if(typeof callback == 'function'){
                            if (result === null) {
                                restClient.put("database/" + databaseKey, content, queryParams, success, error);
                            } else {
                                callback(result.slice(2));
                            }
                        }
                    } else {
                        alert("Database addition failed");
                    }
                };

                restClient.put("database/" + databaseKey, content, queryParams, success, error);
            },
            /**
             * Removes a database of the set of the configured databases of the user currently logged in
             * @param databaseKey The key of the database to delete
             * @param callback Callback, called when the database has been removed successfully. Has one paramter consisting of the database key of the removed database
             */
            removeDatabase: function(databaseKey,callback){
                if(!restClient.loggedIn()) return;

                var queryParams = {
                    format: QV.VISUALIZATIONTYPE.JSON.STRING
                };

                var success = function(result, type, status) {
                    if (typeof result !== "object") {
                        try{
                            result = JSON.parse(result);
                        } catch (e){
                            alert("Error! Failed to parse JSON");
                            return;
                        }
                    }
                    if(result[2] == databaseKey){
                        alert("Database removed!");
                        if(typeof callback == 'function'){
                            if (result === null) {
                                restClient.delete("database/" + databaseKey, null, queryParams, success, error);
                            } else {
                                callback(result.slice(2));
                            }
                        }
                    }
                };

                restClient.delete("database/" + databaseKey, null, queryParams, success, error);
            },
            /**
             * Adds a new filter to the set of the configured filters of the user currently logged in
             * @param filterKey
             * @param query
             * @param databaseKey
             * @param callback Callback, called when the filter has been added successfully. Has one paramter consisting of the filter key of the added filter
             */
            addFilter: function(filterKey,query,databaseKey,callback){
                if(!restClient.loggedIn()) return;

                var content = {
                    query: query
                };

                var queryParams = {
                    format: QV.VISUALIZATIONTYPE.JSON.STRING
                };

                var success = function(result, type, status) {
                    if (typeof result !== "object") {
                        try{
                            result = JSON.parse(result);
                        } catch (e){
                            alert("Error! Failed to parse JSON");
                            return;
                        }
                    }
                    if(result[2] == filterKey){
                        alert("Filter added!");
                        if(typeof callback == 'function'){
                            if (result === null) {
                                restClient.put("filter/" + databaseKey + "/" + filterKey, content, queryParams, success, error);
                            } else {
                                callback(result.slice(2));
                            }
                        }
                    } else {
                        alert("Filter addition failed");
                    }
                };

                restClient.put("filter/" + databaseKey + "/" + filterKey, content, queryParams, success, error);
            },
            /**
             * Removes a filter of the set of the configured filters of the user currently logged in
             * @param filterKey The key of the filter to delete
             * @param callback Callback, called when the filter has been removed successfully. Has one paramter consisting of the filter key of the removed filter
             */
            removeFilter: function(filterKey,callback){
                if(!restClient.loggedIn()) return;

                var queryParams = {
                    format: QV.VISUALIZATIONTYPE.JSON.STRING
                };

                var success = function(result, type, status) {
                    if (typeof result !== "object") {
                        try{
                            result = JSON.parse(result);
                        } catch (e){
                            alert("Error! Failed to parse JSON");
                            return;
                        }
                    }
                    if(result[2] == filterKey){
                        alert("Filter removed!");
                        if(typeof callback == 'function'){
                            if (result === null) {
                                restClient.delete("filter/" + filterKey, null, queryParams, success, error);
                            } else {
                                callback(result.slice(2));
                            }
                        }
                    } else {
                        alert("Filter removal failed");
                    }
                };

                restClient.delete("filter/" + filterKey, null, queryParams, success, error);
            },
            /**
             * Removes a query of the set of the configured query of the user currently logged in
             * @param queryKey The key of the query to delete
             * @param callback Callback, called when the query has been removed successfully. Has one paramter consisting of the query key of the removed query
             */
            removeQuery: function(queryKey,callback){
                if(!restClient.loggedIn()) return;

                var queryParams = {
                    format: QV.VISUALIZATIONTYPE.JSON.STRING
                };

                var success = function(result, type, status) {
                    if (typeof result !== "object") {
                        try{
                            result = JSON.parse(result);
                        } catch (e){
                            alert("Error! Failed to parse JSON");
                            return;
                        }
                    }
                    if(result[2] == queryKey){
                        alert("Query removed!");
                        if(typeof callback == 'function'){
                            if (result === null) {
                                restClient.delete("query/" + queryKey, null, queryParams, success, error);
                            } else {
                                callback(result.slice(2));
                            }
                        }
                    } else {
                        alert("Query removal failed");
                    }
                };

                restClient.delete("query/" + queryKey, null, queryParams, success, error);
            },
            /**
             * Retrieves the keys of the databases configured for the user currently logged in
             * @param callback Callback, called when the keys have been retrieved. Has one paramter consisting of an array of the database keys
             */
            retrieveDatabaseKeys: function(callback){
                if(!restClient.loggedIn()) return;

                var queryParams = {
                    format: QV.VISUALIZATIONTYPE.JSON.STRING
                };

                var success = function(result, type, status) {
                    if (typeof result !== "object") {
                        try{
                            result = JSON.parse(result);
                        } catch (e){
                            log("Error! Failed to parse JSON");
                            return;
                        }
                    }
                    if(typeof callback == 'function'){
                        if (result === null) {
                            restClient.get("database", null, queryParams, success, error);
                        } else {
                            callback(result.slice(2));
                        }
                    }
                };

                restClient.get("database", null, queryParams, success, error);
            },
            /**
             * Retrieves the keys of the filters used in a specified query.
             * @param query the ID of the query
             * @param callback Callback, called when the keys have been retrieved. Has one paramter consisting of an array of the filter keys
             */
            retrieveFilterKeysOfQuery: function(query, callback){
                var queryParams = {
                    format: QV.VISUALIZATIONTYPE.JSON.STRING
                };

                var success = function(result, type, status) {
                    if (typeof result !== "object") {
                        try{
                            result = JSON.parse(result);
                        } catch (e){
                            log("Error! Failed to parse JSON");
                            return;
                        }
                    }
                    if(typeof callback == 'function'){
                        if (result === null) {
                            restClient.get("query/" + query + "/filter", null, queryParams, success, error);
                        } else {
                            callback(result.slice(2));
                        }
                    }
                };

                restClient.get("query/" + query + "/filter", null, queryParams, success, error);
            },
            /**
             * Retrieves the keys of the filters configured for the user currently logged in
             * @param callback Callback, called when the keys have been retrieved. Has one paramter consisting of an array of the filter keys
             */
            retrieveFilterKeys: function(callback){
                if(!restClient.loggedIn()) return;

                var queryParams = {
                    format: QV.VISUALIZATIONTYPE.JSON.STRING
                };

                var success = function(result, type, status) {
                    if (typeof result !== "object") {
                        try{
                            result = JSON.parse(result);
                        } catch (e){
                            log("Error! Failed to parse JSON");
                            return;
                        }
                    }
                    if(typeof callback == 'function'){
                        if (result === null) {
                            restClient.get("filter", null, queryParams, success, error);
                        } else {
                            callback(result.slice(2));
                        }
                    }
                };

                restClient.get("filter", null, queryParams, success, error);
            },
            /**
             * Retrieves the values for a specific filter specified by its key
             * @param filterKey
             * @param callback Callback, called when the values have been retrieved. Has one paramter consisting of an array of the filter values
             */
            retrieveFilterValuesOfUser: function(dbKey, filterKey, user, callback){
                var success = function(result, type, status) {
                    if (typeof result !== "object") {
                        try{
                            result = JSON.parse(result);
                        } catch (e){
                            log("Error! Failed to parse JSON");
                            return;
                        }
                    }
                    if(typeof callback == 'function'){
                        callback(result.slice(2));
                    }
                };

                var queryParams = {
                    format: QV.VISUALIZATIONTYPE.JSON.STRING
                };

                restClient.get("filter/" + dbKey + "/" + filterKey + "/" + user, null, queryParams, success, error);
            },
            /**
             * Retrieves the values for a specific filter specified by its key
             * @param filterKey
             * @param callback Callback, called when the values have been retrieved. Has one paramter consisting of an array of the filter values
             */
            retrieveFilterValues: function(dbKey, filterKey, callback){
                if(!restClient.loggedIn()) return;

                var success = function(result, type, status) {
                    if (typeof result !== "object") {
                        try{
                            result = JSON.parse(result);
                        } catch (e){
                            log("Error! Failed to parse JSON");
                            return;
                        }
                    }
                    if(typeof callback == 'function'){
                        callback(result.slice(2));
                    }
                };

                var queryParams = {
                    format: QV.VISUALIZATIONTYPE.JSON.STRING
                };

                restClient.get("filter/" + dbKey + "/" + filterKey, null, queryParams, success, error);
            },
            /**
             * Retrieves the keys of the queries saved for the user currently logged in
             * @param callback Callback, called when the keys have been retrieved. Has one paramter consisting of an array of the query keys
             */
            retrieveQueryKeys: function(callback){
                if(!restClient.loggedIn()) return;

                var queryParams = {
                    format: QV.VISUALIZATIONTYPE.JSON.STRING
                };

                var success = function(result, type, status) {
                    if (typeof result !== "object") {
                        try{
                            result = JSON.parse(result);
                        } catch (e){
                            log("Error! Failed to parse JSON");
                            return;
                        }
                    }
                    if(typeof callback == 'function'){
                        if (result === null) {
                            restClient.get("query", null, queryParams, success, error);
                        } else {
                            callback(result.slice(2));
                        }
                    }
                };

                restClient.get("query", null, queryParams, success, error);
            },
            /**
             * Retrieves the values for a specific query specified by its key
             * @param query
             * @param callback Callback, called when the values have been retrieved. Has one paramter consisting of an array of the query values
             */
            retrieveQueryValues: function(queryKey,callback){
                if(!restClient.loggedIn()) return;

                var success = function(result, type, status) {
                    if (typeof result !== "object") {
                        try{
                            result = JSON.parse(result);
                        } catch (e){
                            log("Error! Failed to parse JSON");
                            return;
                        }
                    }
                    if(typeof callback == 'function'){
                        callback(result);
                    }
                };

                var queryParams = {
                    format: QV.VISUALIZATIONTYPE.JSON.STRING
                };

                restClient.get("query/" + queryKey, null, queryParams, success, error);
            },
            /**
             * Retrieves the visualization for a query and a set of database and modification options
             * @param query The query string
             * @param queryParams Query Parameters
             * @param databaseKey The identifier of the database to query
             * @param modficationTypeIndex The index of the modification function to apply
             * @param visualizationTypeIndex The index of the visualization type the result should be visualized with
             * @param visualizationOptions An array of options
             * @param outputNode The DOM node to embed the visualization into
             * @param callback Callback function, called when the result has been retrieved. Has one paramter consisting of the retrieved data (visualization's HTML code)
             */
            save: function(query,queryParams,databaseKey,modficationTypeIndex,visualizationTypeIndex,visualizationOptions,outputNode,callback){
                save(query,queryParams,databaseKey,modficationTypeIndex,visualizationTypeIndex,visualizationOptions,false,outputNode,callback);
            },
            /**
             * Retrieves the visualization for a query and a set of database and modification options
             * @param query The query string
             * @param queryParams Query Parameters
             * @param databaseKey The identifier of the database to query
             * @param modficationTypeIndex The index of the modification function to apply
             * @param visualizationTypeIndex The index of the visualization type the result should be visualized with
             * @param visualizationOptions An array of options
             * @param outputNode The DOM node to embed the visualization into
             * @param callback Callback function, called when the result has been retrieved. Has one paramter consisting of the retrieved data (visualization's HTML code)
             */
            retrieve: function(query,queryParams,databaseKey,modficationTypeIndex,visualizationTypeIndex,visualizationOptions,outputNode,callback){
                retrieve(query,queryParams,databaseKey,modficationTypeIndex,visualizationTypeIndex,visualizationOptions,false,outputNode,callback);
            },
            /**
             * Directly retrieves the visualization for a query and a set of database and modification options and some LAS credentials
             * @param username A LAS username
             * @param password A LAS Password
             * @param query The query string
             * @param queryParams Query Parameters
             * @param databaseKey The identifier of the database to query
             * @param modficationTypeIndex The index of the modification function to apply
             * @param visualizationTypeIndex The index of the visualization type the result should be visualized with
             * @param visualizationOptions An array of options
             * @param outputNode The DOM node to embed the visualization into
             */
            quickRetrieve: function(username,password,query,queryParams,databaseKey,modficationTypeIndex,visualizationTypeIndex,visualizationOptions,outputNode){
                this.retrieve(query,queryParams,databaseKey,modficationTypeIndex,visualizationTypeIndex,visualizationOptions,outputNode,function(){
                });
            },
            /**
             * Retrieves the visualization key for a query and a set of database and modification options
             * @param query The query string
             * @param queryParams Query Parameters
             * @param databaseKey The identifier of the database to query
             * @param modficationTypeIndex The index of the modification function to apply
             * @param visualizationTypeIndex The index of the visualization type the result should be visualized with
             * @param visualizationOptions An array of options
             * @param useCache Use cache for query result
             * @param callback Callback function, called when the result has been retrieved. Has one paramter consisting of the retrieved data (visualization key)
             */
            retrieveChartKey: function(query,queryParams,databaseKey,modficationTypeIndex,visualizationTypeIndex,visualizationOptions,useCache,callback){
                save(query,queryParams,databaseKey,modficationTypeIndex,visualizationTypeIndex,visualizationOptions,useCache,null,callback);
            },
            /**
             * Retrieves the visualization for a visualization key
             * @param key The visualization key
             * @param outputNode The DOM node to embed the visualization into
             * @param callback Callback function, called when the result has been retrieved. Has one paramter consisting of the retrieved data (visualization's HTML code)
             * @param filterValues Values to be filled in inside of a parameterized Query
             */
            retrieveFromKey: function(key,outputNode,callback,filterValues){
                retrieveFromKey(key,outputNode,callback,filterValues);
            },
            /**
             * Directly retrieves the visualization for a visualization key and some LAS credentials
             * @param username A LAS username
             * @param password A LAS Password
             * @param key The visualization key
             * @param outputNode The DOM node to embed the visualization into
             * @param filterValues Values to be filled in inside of a parameterized Query
             */
            quickRetrieveFromKey: function(username,password,key,outputNode,filterValues){
                this.retrieveFromKey(key,outputNode,function(){},filterValues);
            },
            /**
             * Directly retrieves the visualization for a visualization key. No unauthentication required.
             * @param key The visualization key
             * @param outputNode The DOM node to embed the visualization into
             * @param filterValues Values to be filled in inside of a parameterized Query
             */
            fromKey: function(key,outputNode,filterValues){
                this.quickRetrieveFromKey(LASUSERNAME,LASPASSWORD,key,outputNode, filterValues);
            },
            /**
             * Directly retrieves the visualization for a visualization key. No unauthentication required.
             * @param key The visualization key
             * @param outputNode The DOM node to embed the visualization into
             */
            getFilterValues: function(database, filter, user,outputNode){
                this.quickRetrieveFromKey(database, filter, user, outputNode);
            },
        };

    };

    /**
     * Shorthand for the unauthenticated retrieval of a visualization based on a visualization key and the direct embedding into the DOM tree at the position where the calling script is placed or in a DOM element selected by id
     * @param key The visualization key
     * @param eleId The (optional) id of a DOM Element
     */
    QV.fromKey = function(key, eleId, filtersEleId, qv){
        if (!eleId) {
            var randomId = QV.HELPER.getRandomId(10,true);
            /* jshint ignore:start */
            document.write('<div id="' + randomId + '" ></div>');
            /* jshint ignore:end */
            eleId = randomId;
        }
        var container = document.getElementById(eleId);
        qv = qv || new QV.Visualizer();
        qv.fromKey(key,container);
        if (filtersEleId) {
            QV.getFilters(key, qv, container, filtersEleId);
        }
        return qv;
    };

    /**
     * Shorthand for the unauthenticated retrieval of filter values based on a database key and filter Name
     * @param database The visualization key
     * @param filter The visualization key
     * @param eleId The (optional) id of a DOM Element
     */
    QV.getFilterValues = function(database, filter, user, eleId, qv){
        if (!eleId) {
            var randomId = QV.HELPER.getRandomId(10,true);
            /* jshint ignore:start */
            document.write('<div id="' + randomId + '" ></div>');
            /* jshint ignore:end */
            eleId = randomId;
        }
        var container = document.getElementById(eleId);
        qv.retrieveFilterValuesOfUser(database, filter, user, function(values) {
            console.log(values);
            var h3 = $('<h3><label for="qv_filter_' + filter +'">' + filter + '</label></h3>');
            var select = $('<select id="qv_filter_' + filter +'"></select>');
            $(container).append(h3);
            $(container).append(select);
            for (var i = 0, len = values.length; i < len; i++) {
                var v = values[i];
                $('<option value="' + v +'">' + v + '</option>').appendTo(select);
            }
            select.on("change", function() {
                qv.trigger({filter: {key: filter, value:this.value}, reload: true});
            });
        });
    };

    QV.getFilters = function(key, qv, container, eleId) {
        if (typeof eleId !== "string") {
            eleId = "qv_filter_div";
        }
        if ($("#"+eleId).length < 1) {
            /* jshint ignore:start */
            document.write('<div id="' + eleId + '" ></div>');
            /* jshint ignore:end */
        }
        qv.retrieveFilterKeysOfQuery(key, function(values) {
            var filters = {};
            for (var i = 0, len = values.length; i < len; i++) {
                var filter = values[i][0];
                var database = values[i][1];
                var user = values[i][2];
                QV.getFilterValues(database, filter, user, eleId, qv);
                filters[filter] = null;
            }
            qv.addVisualization(key, function(event) {
                if (event.filter) {
                    filters[event.filter.key] = event.filter.value;
                }
                var filterArray = [];
                for (var x in filters) {
                    filterArray.push(filters[x]);
                }
                if (event.reload === true) {
                    qv.fromKey(key,container,filterArray);
                }
            });
        });
    };

    return QV;

})(QV || {});
