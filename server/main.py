"""
Modified gae-json-rest code to be REST AppEngine server.
"""
import logging

import csv
import time

import wsgiref.handlers
from google.appengine.api import users
from google.appengine.ext import webapp
import models
import cookutil
import jsonutil
import restutil

def set_logging():
  logger = logging.getLogger()
  logger.setLevel(logging.DEBUG)
  # create console handler and set level to debug
  ch = logging.StreamHandler()
  ch.setLevel(logging.DEBUG)
  # create formatter
  formatter = logging.Formatter("%(asctime)s - %(name)s - "
                                "%(levelname)s - %(message)s")
  # add formatter to ch
  ch.setFormatter(formatter)
  # add ch to logger
  logger.addHandler(ch)
  return logger

logger = set_logging()

class CrudRestHandler(webapp.RequestHandler, cookutil.CookieMixin):
  """
  Handle the REST updates for the Sample resource
  """
  def _serve(self, data):
    counter = self.get_cookie('counter')
    if counter:
      self.set_cookie('counter', str(int(counter) + 1))
    else:
      self.set_cookie('counter', '0')
    return jsonutil.send_json(self.response, data)

  def _get_model_and_entity(self, need_model, need_id, user):
    """ Analyze self.request.path to get model and entity.

    Args:
      need_model: bool: if True, fail if classname is missing
      need_id: bool: if True, fail if ID is missing

    Returns 3-item tuple:
      failed: bool: True iff has failed
      model: class object or None
      entity: instance of model or None
    """
    classname, strid = jsonutil.path_to_classname_and_id(self.request.path)
    self._classname = classname
    if not classname:
      if need_model:
        self.response.set_status(400, 'Cannot do it without a model.')
      return need_model, None, None
    model = restutil.modelClassFromName(classname)
    if model is None:
      self.response.set_status(400, 'Model %r not found' % classname)
      return True, None, None
    if not strid:
      if need_id:
        self.response.set_status(400, 'Cannot do it without an ID.')
      return need_id, model, None
    try:
      numid = int(strid)
    except TypeError:
      self.response.set_status(400, 'ID %r is not numeric.' % strid)
      return True, model, None
    else:
      entity = model.get_by_id(numid)
      if entity is None:
        self.response.set_status(404, "Entity %s not found" % self.request.path)
        return True, model, None
    return False, model, entity
 
  def get(self):
    """ Get JSON data for model names, entity IDs of a model, or an entity.

    Depending on the request path, serve as JSON to the response object:
    - for a path of /classname/id, a jobj for that entity
    - for a path of /classname, a list of id-only jobjs for that model
    - for a path of /, a list of all model classnames
    """
    user = users.get_current_user()
    if user is None:
      return self.redirect(users.create_login_url(self.request.uri))

    coon = str(1 + int(self.get_cookie('coon', '0')))
    self.set_cookie('count', coon)
    self.set_cookie('ts', str(int(time.time())))
    failed, model, entity = self._get_model_and_entity(False, False, user)
    if failed: return
    if model is None:
      return self._serve(restutil.allModelClassNames())
    if entity is None:
      return self._serve([jsonutil.id_of(x) for x in model.all()])
    jobj = jsonutil.make_jobj(entity)
    return self._serve(jobj)

  def post(self):
    """ Create an entity of model given by path /classname.

        Request body is JSON for a jobj for a new entity (without id!).
        Response is JSON for a jobj for a newly created entity.
        Also sets HTTP Location: header to /classname/id for new entity.
    """
    user = users.get_current_user()
    if user is None:
      return self.redirect(users.create_login_url(self.request.uri))
    
    failed, model, entity = self._get_model_and_entity(True, False, user)
    if failed: return
    if entity is not None:
      self.response.set_status(400, 'Cannot create entity with fixed ID.')
      return
    jobj = jsonutil.receive_json(self.request)
    if hasattr(model, 'check_existing'):
      existing = model.check_existing(jobj)
      if existing:
        new_entity_path = "/%s/%d" % (self._classname, jsonutil.id_of(existing)['id'])
        self.response.headers['Location'] = new_entity_path
        self.response.set_status(302, 'Found existing object %s' % new_entity_path)
        return
        
    jobj = jsonutil.make_entity(model, jobj, user=user)
    self._serve(jobj)
    new_entity_path = "/%s/%s" % (self._classname, jobj['id'])
    logging.info('Post created %r', new_entity_path)
    self.response.headers['Location'] = new_entity_path
    self.response.set_status(201, 'Created entity %s' % new_entity_path)

  def put(self):
    """ Update an entity of model given by path /classname/id.

        Request body is JSON for a jobj for an existing entity.
        Response is JSON for a jobj for the updated entity.
    """
    user = users.get_current_user()
    if user is None:
      return self.redirect(users.create_login_url(self.request.uri))
    
    failed, model, entity = self._get_model_and_entity(True, True, user)
    if failed: return
    jobj = jsonutil.receive_json(self.request)
    jobj = jsonutil.update_entity(entity, jobj)
    self._serve(jobj)
    updated_entity_path = "/%s/%s" % (self._classname, jobj['id'])
    self.response.set_status(200, 'Updated entity %s' % updated_entity_path)

  def delete(self):
    """ Delete an entity of model given by path /classname/id.

        Response is JSON for an empty jobj.
    """
    user = users.get_current_user()
    if user is None:
      return self.redirect(users.create_login_url(self.request.uri))
    
    failed, model, entity = self._get_model_and_entity(True, True, user)
    if failed: return
    entity.delete()
    self._serve({})

class LastSampleResource(webapp.RequestHandler, cookutil.CookieMixin):
  "Provide the resource to handle /latest requests"
  
  def get(self):
    """ Get JSON data for model names, entity IDs of a model, or an entity.

    Depending on the request path, serve as JSON to the response object:
    - for a path of /classname/id, a jobj for that entity
    - for a path of /classname, a list of id-only jobjs for that model
    - for a path of /, a list of all model classnames
    """
    user = users.get_current_user()
    if user is None:
      return self.redirect(users.create_login_url(self.request.uri))
    logger.info("getting latest for user %s" % user)
    q = models.Sample.all().filter("user =",user).order("-created")
    results = q.fetch(1)
    if len(results):
      return jsonutil.send_json(self.response, {'created':results[0].created})
    else:
      return jsonutil.send_json(self.response, {})


class ExportResource(webapp.RequestHandler, cookutil.CookieMixin):
  "Provide the resource to handle /export requests"
  
  def get(self):
    """
    Return Text CSV exported data
    """
    # TODO: Refactor user login to decorator
    user = users.get_current_user()
    if user is None:
      return self.redirect(users.create_login_url(self.request.uri))

    logger.info("getting export for user %s" % user)
    q = models.Sample.all().filter("user =",user).order("created")
    self.response.headers['Content-Type'] = 'text/csv'
    
    headers = [p.verbose_name for p in models.Sample.property_list()]

    writer = csv.writer(self.response.out)
    writer.writerow(headers)

    results = q.fetch(1000)
    
    rows = [r.get_values() for r in results]
    writer.writerows(rows)
      
def application():
  return webapp.WSGIApplication([('/latest', LastSampleResource),
                                 ('/export', ExportResource),
                                 ('/.*', CrudRestHandler)],
                                debug=True)
def main():
  logging.info('main.py main()')
  application =  webapp.WSGIApplication([('/latest', LastSampleResource),
                                         ('/export', ExportResource),
                                         ('/.*', CrudRestHandler)],
                                        debug=True)
  wsgiref.handlers.CGIHandler().run(application)

if __name__ == '__main__':
  main()
