""" Json-Rest-handlin' integration helper.

This module offers a JSON+REST-handling integration class meant to be used with
Google App Engine (hooked into a webapp.RequestHandler subclass); it can be
hooked up by simply passing an object h with attributes h.request and
h.response that are duck-like those of webapp.RequestHandler.

On hookup, the integration-helper class overrides the get/set/put/delete
methods of the object hooking up to it so that they respond appropriately to
REST requests (as documented in json_rest.txt) based on registrations performed
in restutil, parsing and formatting JSON payloads based on jsonutil.

IOW, this helper integrates functionality found in other modules of the
gae-json-rest package:
  parsutil
  restutil
  jsonutil
"putting it all together" into a highly-reusable (but still modestly
customizable) REST-style, JSON-transport server web-app for GAE.

TODO: decide what arguments/parameters are passed to various kinds of
methods being called, and implement that decision; add MANY tests!!!

"""
import logging

import jsonutil
import parsutil
import restutil


class JsonRestHelper(object):

  prefix_to_ignore = '/'
  __delete_parser = __put_parser = __post_parser = __get_parser = None

  def hookup(self, handler):
    """ "Hooks up" this helper instance to a handler object.

    Args:
      handler: an instance of a webapp.RequestHandler subclass
    Side effects:
      - sets self.handler to handler
      - sets the handler's get, put, post and delete methods from self
      - sets the handler's jrh attribute to self
    Note this creates reference loops and MUST be undone in hookdown!
    """
    logging.info('hookup %r/%r', self, handler)
    self.handler = handler
    handler.get = self.get
    handler.put = self.put
    handler.post = self.post
    handler.delete = self.delete
    handler.jrh = self

  def hookdown(self):
    """ Undoes the effects of self.hookup """
    logging.info('hookdn %r/%r', self, self.handler)
    h = self.handler
    h.jrh = self.handler = None
    del h.get, h.put, h.post, h.delete

  def _serve(self, data):
    """ Serves a result in JSON, and hooks-down from the handler """
    try: return jsonutil.send_json(self.handler.response, data)
    finally: self.hookdown()

  def get_model(self, modelname):
    """ Gets a model (or None) given a model name.

    Args:
      modelname: a string that should name a model
    Returns:
      a model class, or None (if no model's registered with that name)
    Side effects:
      sets response status to 400 if no model's registered with that name
    """
    model = restutil.modelClassFromName(modelname)
    if model is None:
      self.handler.response.set_status(400, 'Model %r not found' % modelname)
    return model

  def get_special(self, specialname):
    """ Gets a special (or None) given a special object's name.

    Args:
      specialname: a string that should name a special object
    Returns:
      a special object, or None (if no special's registered with that name)
    Side effects:
      sets response status to 400 if no special's registered with that name
    """
    special = restutil.specialFromName(specialname)
    if special is None:
      self.handler.response.set_status(400, 'Special object %r not found' %
                                             specialname)
    return special

  def get_entity(self, modelname, strid):
    """ Gets an entity (or None) given a model name and entity ID as string.

    Args:
      modelname: a string that should name a model
      strid: the str(id) for the numeric id of an entity of that model
    Returns:
      an entity, or None (if something went wrong)
    Side effects:
      sets response status to 400 or 404 if various things went wrong
    """
    model = self.get_model(modelname)
    if model is None:
      return None
    entity = model.get_by_id(int(strid))
    if entity is None:
      self.handler.response.set_status(404, "Entity %s/%s not found" %
                                             (modelname, strid))
    return entity

  def get_special_method(self, specialname, methodname):
    """ Gets a special object method (or None) given special & method names.

    Args:
      specialname: a string that should name a special object
      methodname: a string that should name a method of that special object
    Returns:
      the method with that name in the special object of that name
    Side effects:
      sets response status to 400 if special or method not found
    """
    special = self.get_special(specialname)
    if special is None: return ''
    method = special.get(methodname)
    if method is None:
      self.handler.response.set_status(400, 'Method %r not found in special %r'
                                           % (methodname, specialname))
    return method

  def _methodhelper(self, modelname, methodname, _getter):
    """ Gets a model or instance method given model and method names & getter.

    Args:
      modelname: a string that should name a model
      methodname: a string that should name a method of that model
                  (model-method or instance-method, dep. on _getter)
    Returns:
      a method object, or None if either model or method were not found
    Side effects:
      sets response status to 400 if either model or method were not found
    """
    model = self.get_model(modelname)
    if model is None: return ''
    method = _getter(model, methodname)
    if method is None:
      self.handler.response.set_status(400, 'Method %r not found in model' %
                                             (methodname, modelname))
    return method

  def get_model_method(self, modelname, methodname):
    """ Gets a model's method given model and method names.

    Args:
      modelname: a string that should name a model
      methodname: a sring that should name a method of that model
    Returns:
      a method object, or None if either model or method were not found
    Side effects:
      sets response status to 400 if either model or method were not found
    """
    return self._methodhelper(modelname, methodname, restutil.modelMethodByName)

  def get_instance_method(self, modelname, methodname):
    """ Gets an instance method given model and method names.

    Args:
      modelname: a string that should name a model
      methodname: a sring that should name an instance method of that model
    Returns:
      a method object, or None if either model or method were not found
    Side effects:
      sets response status to 400 if either model or method were not found
    """
    return self._methodhelper(modelname, methodname, restutil.instanceMethodByName)


  def do_delete(self, model, strid):
    """ Hook method to delete an entity given modelname and strid.
    """
    entity = self.get_entity(model, strid)
    if entity is not None:
      entity.delete()
    return {}

  def delete(self, prefix=None):
    """ Delete an entity given by path modelname/strid
        Response is JSON for an empty jobj.
    """
    if self.__delete_parser is None:
      self.__delete_parser = parsutil.RestUrlParser(self.prefix_to_ignore,
          do_model_strid=self.do_delete)
    path = self.handler.request.path
    result = self.__delete_parser.process(path, prefix)
    if result is None or isinstance(result, tuple):
      self.handler.response.set_status(400, 'Invalid URL for DELETE: %r' % path)
    return self._serve(result)

  def do_put(self, model, strid):
    """ Hook method to update an entity given modelname and strid.
    """
    entity = self.get_entity(model, strid)
    if entity is None:
      return {}
    jobj = jsonutil.receive_json(self.handler.request)
    jobj = jsonutil.update_entity(entity, jobj)
    updated_entity_path = "/%s/%s" % (model, jobj['id'])
    self.handler.response.set_status(200, 'Updated entity %s' %
                                           updated_entity_path)
    return jobj

  def put(self, prefix=None):
    """ Update an entity given by path modelname/strid
        Request body is JSON for the needed changes
        Response is JSON for the updated entity.
    """
    if self.__put_parser is None:
      self.__put_parser = parsutil.RestUrlParser(self.prefix_to_ignore,
          do_model_strid=self.do_put)
    path = self.handler.request.path
    result = self.__put_parser.process(path, prefix)
    if result is None or isinstance(result, tuple):
      self.handler.response.set_status(400, 'Invalid URL for POST: %r' % path)
      return self._serve({})
    return self._serve(result)

  def do_post_special_method(self, special, method):
    """ Hook method to call a method on a special object given names.
    """
    themethod = self.get_special_method(special, method)
    if special is None: return ''
    try: return themethod()
    except Exception, e:
      self.handler.response.set_status(400, "Can't call %r/%r: %s" % (
                                             special, method, e))
      return ''

  def do_post_model(self, model):
    """ Hook method to "call a model" (to create an entity)
    """
    themodel = self.get_model(model)
    if themodel is None: return ''
    jobj = jsonutil.receive_json(self.handler.request)
    jobj = jsonutil.make_entity(themodel, jobj)
    self._classname = model
    return jobj

  def do_post_model_method(self, model, method):
    """ Hook method to call a method on a model given s.
    """
    themethod = self.get_model_method(model, method)
    if themethod is None: return ''
    try: return themethod()
    except Exception, e:
      self.handler.response.set_status(400, "Can't call %r/%r: %s" % (
                                             model, method, e))
      return ''

  def do_post_entity_method(self, model, strid, method):
    """ Hook method to call a method on an entity given s and strid.
    """
    themethod = self.get_instance_method(model, method)
    if themethod is None: return ''
    entity = self.get_entity(model, strid)
    if entity is None: return ''
    try: return themethod(entity)
    except Exception, e:
      self.handler.response.set_status(400, "Can't call %r/%r/%r: %s" % (
                                             model, strid, method, e))
      return ''

  def post(self, prefix=None):
    """ Create an entity ("call a model") or perform other non-R/O "call".

        Request body is JSON for the needed entity or other call "args".
        Response is JSON for the updated entity (or "call result").
    """
    if self.__post_parser is None:
      self.__post_parser = parsutil.RestUrlParser(self.prefix_to_ignore,
          do_special_method=self.do_post_special_method,
          do_model=self.do_post_model,
          do_model_method=self.do_post_model_method,
          do_model_strid_method=self.do_post_entity_method,
          )
    import ipdb; ipdb.set_trace()
    path = self.handler.request.path
    result = self.__post_parser.process(path, prefix)
    if result is None or isinstance(result, tuple):
      self.handler.response.set_status(400, 'Invalid URL for POST: %r' % path)
      return self._serve({})
    try:
      strid = result['id']
    except (KeyError, AttributeError, TypeError):
      pass
    else:
      new_entity_path = "/%s/%s" % (self._classname, strid)
      logging.info('Post (%r) created %r', path, new_entity_path)
      self.handler.response.headers['Location'] = new_entity_path
      self.handler.response.set_status(201, 'Created entity %s' %
                                             new_entity_path)
    return self._serve(result)

  def do_get_special_method(self, special, method):
    """ Hook method to R/O call a method on a special object given names.
    """
    themethod = self.get_special_method(special, method)
    if themethod is None: return ''
    try: return themethod()
    except Exception, e:
      self.handler.response.set_status(400, "Can't call %r/%r: %s" % (
                                             special, method, e))
      return ''

  def do_get_model(self, model):
    """ Hook method to R/O "call a model" ("get list of all its IDs"...?)
    """
    themodel = self.get_model(model)
    if themodel is None: return ''
    return [jsonutil.id_of(x) for x in themodel.all()]

  def do_get_entity(self, model, strid):
    """ Hook method to get data about an entity given model name and strid
    """
    entity = self.get_entity(model, strid)
    if entity is None:
      return {}
    return jsonutil.make_jobj(entity)

  def do_get_model_method(self, model, method):
    """ Hook method to R/O call a method on a model given s.
    """
    themethod = self.get_model_method(model, method)
    if themethod is None: return ''
    try: return themethod()
    except Exception, e:
      self.handler.response.set_status(400, "Can't call %r/%r: %s" % (
                                             model, method, e))
      return ''

  def do_get_entity_method(self, model, strid, method):
    """ Hook method to R/O call a method on an entity given s and strid.
    """
    themethod = self.get_instance_method(model, method)
    if themethod is None: return ''
    entity = self.get_entity(model, strid)
    if entity is None: return ''
    try: return themethod(entity)
    except Exception, e:
      self.handler.response.set_status(400, "Can't call %r/%r/%r: %s" % (
                                             model, strid, method, e))
      return ''

  def get(self, prefix=None):
    """ Get JSON data for entity IDs of a model, or all about an entity.

    Depending on the request path, serve as JSON to the response object:
    - for a path of /classname/id, a jobj for that entity
    - for a path of /classname, a list of id-only jobjs for that model
    - or, the results of the method being called (should be R/O!)
    """
    logging.info('GET path=%r, prefix=%r', self.handler.request.path, prefix)
    if self.__get_parser is None:
      self.__get_parser = parsutil.RestUrlParser(self.prefix_to_ignore,
          do_special_method=self.do_get_special_method,
          do_model=self.do_get_model,
          do_model_strid=self.do_get_entity,
          do_model_method=self.do_get_model_method,
          do_model_strid_method=self.do_get_entity_method,
          )
    path = self.handler.request.path

    # hacky/kludgy special-case: serve all model names (TODO: remove this!)
    # (need to have proper %meta special w/methods to get such info!)
    if prefix is not None and path.strip('/') == prefix.strip('/'):
      result = restutil.allModelClassNames()
      logging.info('Hacky case (%r): %r', path, result)
      return self._serve(result)

    result = self.__get_parser.process(path, prefix)
    if result is None or isinstance(result, tuple):
      self.handler.response.set_status(400, 'Invalid URL for GET: %r' % path)
      return self._serve({})
    return self._serve(result)

# expose a single helper object, shd be reusable

helper = JsonRestHelper()

# just for testing...:
import wsgiref.handlers
from google.appengine.ext import webapp
import models

class _TestCrudRestHandler(webapp.RequestHandler):
  def __init__(self, *a, **k):
    webapp.RequestHandler.__init__(self, *a, **k)
    helper.hookup(self)

def main():
  logging.info('intgutil test main()')
  application = webapp.WSGIApplication([('/(rest)/.*', _TestCrudRestHandler)],
      debug=True)
  wsgiref.handlers.CGIHandler().run(application)

if __name__ == '__main__':
  main()
