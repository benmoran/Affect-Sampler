""" Utilities for JSON REST CRUD support for GAE db models.

Terminology: a subclass of db.Model is known as "a Model"; an instance of
such a subclass is known as "an entity".

Data is said to be in JSONed or JSONable form if it contains only dicts, lists
and scalars (strings, numbers) in a form that is correctly serializable into a
JSON-format string.

In particular, a "jobj" is a JSONed dict with a key 'id' mapping the string
format of the numeric value of an entity; each other key must be the name of
a property of that entity's Model, and the corresponding value must be a string
that can be deserialized into a value of that property's type.
"""
import re

import restutil
#from django.utils import simplejson
import simplejson

def id_of(entity):
  """ Make a {'id': <string-of-digits>} dict for an entity.

  Args:
    entity: an entity
  Returns:
    a jobj corresponding to the entity
  """
  return dict(id=restutil.id_of(entity))


# RE to match: optional /, classname, optional /, ID of 0+ numeric digits
CLASSNAME_ID_RE = re.compile(r'^/?(\w+)/?(\d*)$')

def path_to_classname_and_id(path):
  """ Get a (classname, id) pair from a path.

  Args:
    path: a path string to anaylyze
  Returns:
    a 2-item tuple:
      (None, '')            if the path does not match CLASSNAME_ID_RE
      (classname, idstring) if the path does match
                            [idstring may be '', or else a string of digits]
  """
  mo = CLASSNAME_ID_RE.match(path)
  if mo: return mo.groups()
  else: return (None, '')


def send_json(response_obj, jdata):
  """ Send data in JSON form to an HTTP-response object.

  Args:
    response_obj: an HTTP response object
    jdata: a dict or list in correct 'JSONable' form
  Side effects:
    sends the JSON form of jdata on response.out
  """
  response_obj.headers['Content-Type'] = 'application/json'
  simplejson.dump(jdata, response_obj.out)


def receive_json(request_obj):
  """ Receive data in JSON form from an HTTP-request object.

  Args:
    request_obj: an HTTP request object (with body in JSONed form)
  Returns:
    the JSONable-form result of loading the request's body
  """
  return simplejson.loads(request_obj.body)


def make_jobj(entity):
  """ Make a JSONable dict (a jobj) given an entity.

  Args:
    entity: an entity
  Returns:
    the JSONable-form dict (jobj) for the entity
  """
  model = type(entity)
  jobj = id_of(entity)
  props = restutil.allProperties(model)
  for property_name, property_value in props:
    value_in_entity = getattr(entity, property_name, None)
    if value_in_entity is not None:
      to_string = getattr(model, property_name + '_to_string')
      jobj[property_name] = to_string(value_in_entity)
  return jobj


def parse_jobj(model, jobj):
  """ Make dict suitable for instantiating model, given a jobj.

  Args:
    model: a Model
    jobj: a jobj
  Returns:
    a dict d such that calling model(**d) properly makes an entity
  """
  result = dict()
  for property_name, property_value in jobj.iteritems():
    # ensure we have an ASCII string, not a Unicode one
    property_name = str(property_name)
    from_string = getattr(model, property_name + '_from_string')
    property_value = from_string(property_value)
    if property_value is not None:
      result[property_name] = property_value
  return result


def make_entity(model, jobj, **kwargs):
  """ Makes an entity whose type is model with the state given by jobj.

  Args:
    model: a Model
    jobj: a jobj
  Side effects:
    creates and puts an entity of type model, w/state per jobj
  Returns:
    a jobj representing the newly created entity
  """
  entity_dict = parse_jobj(model, jobj)
  entity_dict.update(kwargs)
  entity = model(**entity_dict)
  entity.put()
  jobj = make_jobj(entity)
  jobj.update(id_of(entity))
  return jobj


def update_entity(entity, jobj):
  """ Updates an entity's state as per properties given in jobj.

  Args:
    entity: an entity
    jobj: a jobj
  Side effects:
    updates the entity with properties as given by jobj
  Returns:
    a jobj representing the whole new state of the entity
  """
  new_entity_data = parse_jobj(type(entity), jobj)
  for property_name, property_value in new_entity_data.iteritems():
    setattr(entity, property_name, property_value)
  entity.put()
  return make_jobj(entity)
