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
        CSV: 0,
        JSON: 1,
        HTMLTABLE: 2,
        XML: 3,
        GOOGLEPIECHART: 4,
        GOOGLEBARCHART: 5,
        GOOGLELINECHART: 6,
        GOOGLETIMELINECHART: 7,
        GOOGLERADARCHART: 8,
        GOOGLETABLE: 9,

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
     * Enum of the supported Database Types
     * @type {Object}
     */
    QV.DATABASETYPE = {
        DB2:        {ID: 1, DEFAULTPORT: 50000},
        MYSQL:      {ID: 2, DEFAULTPORT: 3306},
        FIREBIRD:   {ID: 3, DEFAULTPORT: 3050},
        MSSQL:      {ID: 4, DEFAULTPORT: 1433},
        POSTGRESQL: {ID: 5, DEFAULTPORT: 5432},
        DERBY:      {ID: 6, DEFAULTPORT: 1527},
        ORACLE:     {ID: 7, DEFAULTPORT: 1521},

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
     * The Query Visualization Library
     * @return {Object}
     */
    QV.Visualizer = function(){

        /* * *                  *
         *  private properties  *
         *                  * * */

        var LASHOST = "http://localhost:8080/";
        var LASSERVICENAME = "i5.las2peer.services.queryVisualization.QueryVisualizationService";
        var LASUSERNAME = "anonymous";
        var LASPASSWORD = "anonymous";

        var lasClient;
        var loginCallback = function(){};

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
         * Retrieves the visualization resp. the visualization Key for a query and a set of database and modification options
         * @param query The query string
         * @param queryParams Query Parameters
         * @param databaseKey The identifier of the database to query
         * @param modficationTypeIndex The index of the modification function to apply
         * @param visualizationTypeIndex The index of the visualization type the result should be visualized with
         * @param visualizationOptions An array of
         * @param useCache Use cache for query
         * @param outputNode The DOM node to embed the visualization into
         * @param save Sets if the query should be visualized (save = false) or a key should be generated for it (save = true)
         * @param callback Callback function, called when the result has been retrieved. Has one paramter consisting of the retrieved data (visualization's HTML code or key)
         */
        var retrieve = function(query,queryParams,databaseKey,modficationTypeIndex,visualizationTypeIndex,visualizationOptions,useCache,outputNode,save,callback){
            if(lasClient.getStatus() == "loggedIn"){
                var params = [],
                    paramQuery = {},
                    paramQueryParams = {},
                    paramDBKey = {},
                    paramUseCache = {},
                    paramVisualizationType = {},
                    paramModificationType = {},
                    paramVisualizationParams = {},
                    paramSave = {};

                paramQuery.type = "String";
                paramQuery.value = query;
                paramQueryParams.type = "String[]";
                paramQueryParams.value = queryParams;
                paramDBKey.type = "String";
                paramDBKey.value = databaseKey;
                paramUseCache.type = "boolean";
                paramUseCache.value = useCache;
                paramModificationType.type = "int";
                paramModificationType.value = modficationTypeIndex;
                paramVisualizationType.type = "int";
                paramVisualizationType.value = visualizationTypeIndex;
                paramVisualizationParams.type = "String[]";
                paramVisualizationParams.value = visualizationOptions;
                paramSave.type = "boolean";
                paramSave.value = save;

                params.push(paramQuery,paramQueryParams,paramDBKey,paramUseCache,paramModificationType,paramVisualizationType,paramVisualizationParams,paramSave);

                lasClient.invoke(LASSERVICENAME,"createQuery",params,function(status, result) {
                    var output;
                    if(status == 200 || status == 204) {
                        if(!save && outputNode){
                            output = result.value;
                            if(/^The Query has lead to an error./.test(output)){
                                output = "<p>The Query has lead to an error. See console for more info.</p>";
                                console.log(result.value);
                            }
                            outputNode.innerHTML = QV.HELPER.stripAndExecuteScript(" " + output);
                        }
                        if(typeof callback == 'function'){
                            callback(result.value);
                        }
                    } else {
                        outputNode.innerHTML = "Error! Message: " + result;
                    }
                });
            }
        };

        /**
         * Retrieves the visualization for a query key
         * @param key The query key
         * @param outputnode The DOM node to embed the visualization into
         * @param callback Callback function, called when the result has been retrieved. Has one paramter consisting of the retrieved data (visualization's HTML code)
         */
        var retrieveFromKey = function(key,outputnode,callback){
            if(lasClient.getStatus() == "loggedIn"){
                var params = [],
                    paramKey = {};

                paramKey.type = "String";
                paramKey.value = key;

                params.push(paramKey);

                lasClient.invoke(LASSERVICENAME,"visualizeQuery",params,function(status, result) {
                    if(status == 200 || status == 204) {
                        outputnode.innerHTML = QV.HELPER.stripAndExecuteScript(" " + result.value);
                        if(typeof callback == 'function'){
                            callback(result.value);
                        }
                    } else {
                        outputnode.innerHTML = "Error! Message: " + result;
                    }
                });
            }
        };

        /* * *           *
         *  constructor  *
         *           * * */

        lasClient = new LasAjaxClient("QueryVisualizer", function(statusCode, message) {
            switch(statusCode) {
                case Enums.Feedback.LoginSuccess:
                    log("Login successful!");
                    loginCallback();
                    break;
                case Enums.Feedback.LogoutSuccess:
                    log("Logout successful!");
                    break;
                case Enums.Feedback.LoginError:
                case Enums.Feedback.LogoutError:
                    log("Login error: " + statusCode + ", " + message);
                    break;
                case Enums.Feedback.InvocationWorking:
                case Enums.Feedback.InvocationSuccess:
                case Enums.Feedback.Warning:
                    //log(statusCode + ", " + message);
                    break;
                case Enums.Feedback.PingSuccess:
                    //log(statusCode + ", " + message);
                    break;
                default:
                    log("Unhandled Error: " + statusCode + ", " + message);
                    break;
            }
        });

        /* * *              *
         *  public methods  *
         *              * * */

        return {
            /**
             * Logs the passed LAS user in
             * @param username LAS username
             * @param password LAS password
             * @param callback Callback, called when user has been logged in successfully
             */
            login: function(username, password, callback){
                if(typeof callback == "function"){
                    loginCallback = callback;
                }
                if(lasClient.getStatus() == "loggedIn"){
                    loginCallback();
                } else {
                    lasClient.login(username, password, LASHOST, "queryvisualizer");
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
                if(lasClient.getStatus() == "loggedIn"){
                    var params = [],
                        paramDBKey = {},
                        paramTypeCode = {},
                        paramUsername = {},
                        paramPassword = {},
                        paramDatabase = {},
                        paramHost = {},
                        paramPort = {},
                        paramVisualizationType = {};

                    paramDBKey.type = "String";
                    paramDBKey.value = databaseKey;
                    paramTypeCode.type = "int";
                    paramTypeCode.value = databaseTypeCode;
                    paramUsername.type = "String";
                    paramUsername.value = username;
                    paramPassword.type = "String";
                    paramPassword.value = password;
                    paramDatabase.type = "String";
                    paramDatabase.value = database;
                    paramHost.type = "String";
                    paramHost.value = host;
                    paramPort.type = "int";
                    paramPort.value = port;
                    paramVisualizationType.type = "int";
                    paramVisualizationType.value = QV.VISUALIZATIONTYPE.JSON;
                    params.push(paramDBKey,paramTypeCode,paramUsername,paramPassword,paramDatabase,paramHost,paramPort,paramVisualizationType);

                    lasClient.invoke(LASSERVICENAME,"addDatabase",params,function(status, result) {
                        if(status == 200 || status == 204) {
                            try{
                                result.value = JSON.parse(result.value);
                            } catch (e){
                                alert("Error! Failed to parse JSON");
                                return;
                            }
                            if(result.value[2] == databaseKey){
                                alert("Database added!");
                                if(typeof callback == 'function'){
                                    callback(result.value.slice(2));
                                }
                            } else {
                                alert("Database addition failed");
                            }
                        } else {
                            alert("Error! Message: " + result);
                        }
                    });
                }
            },
            /**
             * Removes a database of the set of the configured databases of the user currently logged in
             * @param databaseKey The key of the database to delete
             * @param callback Callback, called when the database has been removed successfully. Has one paramter consisting of the database key of the removed database
             */
            removeDatabase: function(databaseKey,callback){
                if(lasClient.getStatus() == "loggedIn"){
                    var params = [],
                        paramDBKey = {},
                        paramVisualizationType = {};

                    paramDBKey.type = "String";
                    paramDBKey.value = databaseKey;
                    paramVisualizationType.type = "int";
                    paramVisualizationType.value = QV.VISUALIZATIONTYPE.JSON;

                    params.push(paramDBKey,paramVisualizationType);

                    lasClient.invoke(LASSERVICENAME,"removeDatabase",params,function(status, result) {
                        if(status == 200 || status == 204) {
                            try{
                                result.value = JSON.parse(result.value);
                            } catch (e){
                                alert("Error! Failed to parse JSON");
                                return;
                            }
                            if(result.value[2] == databaseKey){
                                alert("Database removed!");
                                if(typeof callback == 'function'){
                                    callback(result.value.slice(2));
                                }
                            } else {
                                alert("Database removal failed");
                            }
                        } else {
                            alert("Error! Message: " + result);
                        }
                    });
                }
            },
            /**
             * Adds a new filter to the set of the configured filters of the user currently logged in
             * @param filterKey
             * @param query
             * @param databaseKey
             * @param callback Callback, called when the filter has been added successfully. Has one paramter consisting of the filter key of the added filter
             */
            addFilter: function(filterKey,query,databaseKey,callback){
                if(lasClient.getStatus() == "loggedIn"){
                    var params = [],
                        paramFilterKey = {},
                        paramQuery= {},
                        paramDatabase = {},
                        paramVisualizationType = {};

                    paramFilterKey.type = "String";
                    paramFilterKey.value = filterKey;
                    paramQuery.type = "String";
                    paramQuery.value = query;
                    paramDatabase.type = "String";
                    paramDatabase.value = databaseKey;
                    paramVisualizationType.type = "int";
                    paramVisualizationType.value = QV.VISUALIZATIONTYPE.JSON;
                    params.push(paramFilterKey,paramQuery,paramDatabase,paramVisualizationType);

                    lasClient.invoke(LASSERVICENAME,"addFilter",params,function(status, result) {
                        if(status == 200 || status == 204) {
                            try{
                                result.value = JSON.parse(result.value);
                            } catch (e){
                                alert("Error! Failed to parse JSON");
                                return;
                            }
                            if(result.value[2] == filterKey){
                                alert("Filter added!");
                                if(typeof callback == 'function'){
                                    callback(result.value.slice(2));
                                }
                            } else {
                                alert("Filter addition failed");
                            }
                        } else {
                            alert("Error! Message: " + result);
                        }
                    });
                }
            },
            /**
             * Removes a filter of the set of the configured filters of the user currently logged in
             * @param filterKey The key of the filter to delete
             * @param callback Callback, called when the filter has been removed successfully. Has one paramter consisting of the filter key of the removed filter
             */
            removeFilter: function(filterKey,callback){
                if(lasClient.getStatus() == "loggedIn"){
                    var params = [],
                        paramFilterKey = {},
                        paramVisualizationType = {};

                    paramFilterKey.type = "String";
                    paramFilterKey.value = filterKey;
                    paramVisualizationType.type = "int";
                    paramVisualizationType.value = QV.VISUALIZATIONTYPE.JSON;

                    params.push(paramFilterKey,paramVisualizationType);

                    lasClient.invoke(LASSERVICENAME,"deleteFilter",params,function(status, result) {
                        if(status == 200 || status == 204) {
                            try{
                                result.value = JSON.parse(result.value);
                            } catch (e){
                                alert("Error! Failed to parse JSON");
                                return;
                            }
                            if(result.value[2] == filterKey){
                                alert("Filter removed!");
                                if(typeof callback == 'function'){
                                    callback(result.value.slice(2));
                                }
                            } else {
                                alert("Filter removal failed");
                            }
                        } else {
                            alert("Error! Message: " + result);
                        }
                    });
                }
            },
            /**
             * Retrieves the keys of the databases configured for the user currently logged in
             * @param callback Callback, called when the keys have been retrieved. Has one paramter consisting of an array of the database keys
             */
            retrieveDatabaseKeys: function(callback){
                if(lasClient.getStatus() == "loggedIn"){
                    var params = [],
                        paramVisualizationType = {};

                    paramVisualizationType.type = "int";
                    paramVisualizationType.value = QV.VISUALIZATIONTYPE.JSON;

                    params.push(paramVisualizationType);

                    lasClient.invoke(LASSERVICENAME,"getDatabaseKeys",params,function(status, result) {
                        if(status == 200 || status == 204) {
                            try{
                                result.value = JSON.parse(result.value);
                            } catch (e){
                                log("Error! Failed to parse JSON");
                                return;
                            }
                            if(typeof callback == 'function'){
                                callback(result.value.slice(2));
                            }
                        } else {
                            log("Error! Message: " + result);
                        }
                    });
                }
            },
            /**
             * Retrieves the keys of the filters configured for the user currently logged in
             * @param callback Callback, called when the keys have been retrieved. Has one paramter consisting of an array of the filter keys
             */
            retrieveFilterKeys: function(callback){
                if(lasClient.getStatus() == "loggedIn"){
                    var params = [],
                        paramVisualizationType = {};

                    paramVisualizationType.type = "int";
                    paramVisualizationType.value = QV.VISUALIZATIONTYPE.JSON;

                    params.push(paramVisualizationType);

                    lasClient.invoke(LASSERVICENAME,"getFilterKeys",params,function(status, result) {
                        if(status == 200 || status == 204) {
                            try{
                                result.value = JSON.parse(result.value);
                            } catch (e){
                                log("Error! Failed to parse JSON");
                                return;
                            }
                            if(typeof callback == 'function'){
                                callback(result.value.slice(2));
                            }
                        } else {
                            log("Error! Message: " + result);
                        }
                    });
                }
            },
            /**
             * Retrieves the values for a specific filter specified by its key
             * @param filterKey
             * @param callback Callback, called when the values have been retrieved. Has one paramter consisting of an array of the filter values
             */
            retrieveFilterValues: function(filterKey,callback){
                if(lasClient.getStatus() == "loggedIn"){
                    var params = [],
                        paramFilterKey = {},
                        paramVisualizationType = {};

                    paramFilterKey.type = "String";
                    paramFilterKey.value = filterKey;
                    paramVisualizationType.type = "int";
                    paramVisualizationType.value = QV.VISUALIZATIONTYPE.JSON;

                    params.push(paramFilterKey,paramVisualizationType);

                    lasClient.invoke(LASSERVICENAME,"getFilterValues",params,function(status, result) {
                        if(status == 200 || status == 204) {
                            try{
                                result.value = JSON.parse(result.value);
                            } catch (e){
                                log("Error! Failed to parse JSON");
                                return;
                            }
                            if(typeof callback == 'function'){
                                callback(result.value.slice(2));
                            }
                        } else {
                            log("Error! Message: " + result);
                        }
                    });
                }
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
                retrieve(query,queryParams,databaseKey,modficationTypeIndex,visualizationTypeIndex,visualizationOptions,false,outputNode,false,callback);
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
                var that = this;
                this.login(username,password,function(){
                    that.retrieve(query,queryParams,databaseKey,modficationTypeIndex,visualizationTypeIndex,visualizationOptions,outputNode,function(){
                        //wait some time until the automatic ping of the LASAjaxClient triggered on every invocation was successful, then logout
                        setTimeout(function(){
                            that.logout();
                        },2000);
                    });
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
                retrieve(query,queryParams,databaseKey,modficationTypeIndex,visualizationTypeIndex,visualizationOptions,useCache,null,true,callback);
            },
            /**
             * Retrieves the visualization for a visualization key
             * @param key The visualization key
             * @param outputNode The DOM node to embed the visualization into
             * @param callback Callback function, called when the result has been retrieved. Has one paramter consisting of the retrieved data (visualization's HTML code)
             */
            retrieveFromKey: function(key,outputNode,callback){
                retrieveFromKey(key,outputNode,callback)
            },
            /**
             * Directly retrieves the visualization for a visualization key and some LAS credentials
             * @param username A LAS username
             * @param password A LAS Password
             * @param key The visualization key
             * @param outputNode The DOM node to embed the visualization into
             */
            quickRetrieveFromKey: function(username,password,key,outputNode){
                var that = this;
                this.login(username,password,function(){
                    that.retrieveFromKey(key,outputNode,function(){
                        //wait some time until the automatic ping of the LASAjaxClient triggered on every invocation was successful, then logout
                        setTimeout(function(){
                            that.logout();
                        },2000);
                    });
                })
            },
            /**
             * Directly retrieves the visualization for a visualization key. No unauthentication required.
             * @param key The visualization key
             * @param outputNode The DOM node to embed the visualization into
             */
            fromKey: function(key,outputNode){
                this.quickRetrieveFromKey(LASUSERNAME,LASPASSWORD,key,outputNode);
            },
            /**
             * Logs out the user currently logged in
             */
            logout: function(){
                lasClient.logout();
            }
        }

    };

    /**
     * Shorthand for the unauthenticated retrieval of a visualization based on a visualization key and the direct embedding into the DOM tree at the position where the calling script is placed
     * @param key The visualization key
     */
    QV.fromKey = function(key){
        var randomId = QV.HELPER.getRandomId(10,true);
        document.write('<div id="' + randomId + '" ></div>');
        var container = document.getElementById(randomId);
        var qv = new QV.Visualizer();
        qv.fromKey(key,container);
    };

    return QV;

})(QV || {});