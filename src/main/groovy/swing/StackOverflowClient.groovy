/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
/*
 * StackOverflowClient.groovy - an example of a blog client using SwingBuilder
 *
 * Based on BlogLinesClient a groovy swing based tool to view blogs using the
 * bloglines aggregator and protocol.  Both of which have past end-of-life.
 *
 * This incarnation, attempts to illustrate the same general concepts using
 * WordPress' REST interface and StackOverflow's blog.
 *
 * Written by Bill Stumbo <wstumbo@charter.net>, January 2021.
 *
 * The original tool was written by Marc Hedlund <marc@precipice.org>, September 2004.
 * 
 * Mangled by John Wilson September 2004
 *
 * Small adaptions to JSR Version by Dierk Koenig, June 2005
 *
 * Used in Marc's article at:
 *    http://www.oreillynet.com/pub/a/network/2004/09/28/bloglines.html
 *
 * Requirements:
 *   - install Groovy as detailed at <http://groovy.apache.org/>.
 *   - put httpcore-4.4.13.jar and httpclient-4.5.13.jar into GROOVY_HOME/lib
 *       see <http://hc.apache.org/downloads.cgi>.
 *
 * To Launch:
 *   groovy StackOverflowClient.groovy
 *
 * This work is licensed under the Creative Commons Attribution
 * License. To view a copy of this license, visit
 * <http://creativecommons.org/licenses/by/2.0/> or send a letter to
 * Creative Commons, 559 Nathan Abbott Way, Stanford, California 94305, USA.
 */
package swing

import groovy.json.JsonSlurper
import groovy.swing.SwingBuilder
import org.apache.http.HttpException
import org.apache.http.HttpResponse
import org.apache.http.client.utils.URIBuilder

import javax.swing.JList
import java.awt.BorderLayout
import javax.swing.JSplitPane
import javax.swing.JTree
import javax.swing.ListSelectionModel
import javax.swing.WindowConstants
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.client.methods.HttpGet

//Set up global variables and data types
server = 'stackoverflow.blog'
restEndpoint = 'wp-json/wp/v2'

class Tag {
    def name
    def id
    def count

    String toString() { (count == "0" ? name : "${name} (${count})") }
}

class Item {
    def title
    def description
    def content

    String toString() { title }
}

//Use HTTPClient for web requests
client = HttpClientBuilder.create().build()

wordPressRestCall = { method, Map<String, String> parameters ->
    url = "https://${server}/${restEndpoint}/${method}"

    try {
        uriBuilder = new URIBuilder(url)
        parameters.entrySet().forEach(e -> uriBuilder.addParameter(e.getKey(), e.getValue()))

        HttpGet getRequest = new HttpGet(uriBuilder.build())
        getRequest.addHeader("accept", "application/json")
        HttpResponse response = client.execute(getRequest)
        // Check to see if the call failed
        if (response.getStatusLine().getStatusCode() >= 400) {
            String message = "response code: " + get.getStatusCode()
            throw(new HttpException(message))
        }
        return response
    } catch (Exception e) {
        message = "Error retrieving <${url}>: ${e}"
        log.info(message)
        log.info(e.getMessage())
    }
}

getAll = {method, Map<String, String> params, int pageNumber ->
    //Map<String, String> params;
    if (params == null) {
        params = new HashMap<>()
    }
    params.put("page", pageNumber.toString())
    //String params = "?page=" + pageNumber
    results = wordPressRestCall.call(method, params)
    count = Integer.valueOf(results.getHeaders("X-WP-TotalPages")[0].getValue())

    if (pageNumber < count) {
        jsonArray = new JsonSlurper().parse(results.getEntity().getContent())  + getAll.call(method, params, pageNumber + 1)

        return jsonArray
    } else {
        return(new JsonSlurper().parse(results.getEntity().getContent()))
    }

}

callBloglinesListsub = getAll.curry('tags', null, 1)

jsonArray = callBloglinesListsub()

//Descend into the subscription outline, adding to the feed tree as we go
treeTop = new DefaultMutableTreeNode("StackOverflow Blog Topics")
parseOutline(jsonArray, treeTop)

def parseOutline(parsedJSON, treeLevel) {
    parsedJSON.each { outline ->
        if (outline != null && outline['name'] != null) {  // this is a tage
            feed = new Tag(name: outline['name'],
                           id: outline['id'],
                           count: outline['count'])
            treeLevel.add(new DefaultMutableTreeNode(feed))
        }
    }

//Build the base user interface objects and configure them
    swing = new SwingBuilder()
    feedTree = new JTree(treeTop)
    itemList = swing.list()
    itemText = swing.textPane(contentType: 'text/html', editable: false)
    model = feedTree.selectionModel
    model.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    itemList.selectionMode = ListSelectionModel.SINGLE_SELECTION

//Set up the action closures that will react to user selections
    listItems = { feed ->
        Map<String, String> params = new HashMap<>();
        params.put("tags", feed.id.toString())
        rssStream = getAll.call("posts", params, 1)
        Vector<Item> posts = new ArrayList<>()
        rssStream.each{ post ->
            posts.add(new Item(title: post['title'].getAt('rendered'), description: post['content'].getAt('rendered')))
        }
        itemList.listData = posts
    }

    feedTree.valueChanged = { event ->
        itemText.text = ""  // clear any old item text
        node = (DefaultMutableTreeNode) feedTree.getLastSelectedPathComponent()
        if (node != null) {
            feed = node.userObject
            if (feed instanceof Tag && feed.count != "0") {
                listItems(feed)
            }
        }
    }

    itemList.valueChanged = { event ->
        item = event.source.selectedValue
        if (item instanceof Item && item?.description != null) {
            itemText.text = "<html><body>${item.description}</body></html>"
        }
    }

//Put the user interface together and display it
    gui = swing.frame(title: 'StackOverflow Blog Tags', location: [100, 100], size: [800, 600],
            defaultCloseOperation: WindowConstants.EXIT_ON_CLOSE) {
        panel(layout: new BorderLayout()) {
            splitPane(orientation: JSplitPane.HORIZONTAL_SPLIT, dividerLocation: 200) {
                scrollPane {
                    widget(feedTree)
                }
                splitPane(orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation: 150) {
                    scrollPane(constraints: BorderLayout.CENTER) {
                        widget(itemList)
                    }
                    scrollPane(constraints: BorderLayout.CENTER) {
                        widget(itemText)
                    }
                }
            }
        }
    }

    gui.setVisible(true)
}