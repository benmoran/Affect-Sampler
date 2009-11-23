"""
Data model in Google Appengine DB terms.
"""
import logging
from google.appengine.ext import db

import jsonutil
import restutil
from datetime import datetime

import logging

class Sample(db.Model):
  """Models an Affect Sample.

  Attributes:
  
    emotion
    intensity
    creation_date
    scheduled_date
    comment
  """
  
  user = db.UserProperty()

  emotion = db.FloatProperty(required=True,
                             verbose_name="Emotion")

  intensity = db.FloatProperty(required=True,
                             verbose_name="Intensity")

  created = db.IntegerProperty(required=True,
                             verbose_name="Created")
                               
  scheduled = db.IntegerProperty(required=False,
                                 verbose_name="Scheduled")
  
  comment = db.StringProperty(required=False,
                              multiline=True,
                              verbose_name="Comment")                                 

  @classmethod
  def check_existing(cls, jsonobj):
    key = jsonobj.get('created')

    logging.info('querying models for %d' % key)
    query = cls.all().filter('created =', key)
    results = query.fetch(1)
    if len(results):
      return results[0]
    return None

  @classmethod
  def property_list(cls):
    """
    Get the ordered list of fields for use in export
    """
    props = cls.properties().values()
    props = [p for p in props if not p.name == 'user']
    props.sort(key=lambda p:p.creation_counter)
    return props

  def get_values(self):
    """
    Return a list of values from the model suitable
    for export.
    """
    return [self.get_value(p) for p in self.property_list()]

  def get_value(self, property):
    """
    Get a single property value, returning as datetime
    for created and scheduled fields.
    """
    
    v = getattr(self, property.name)
    if property.name in ('created','scheduled',):
      v = datetime.fromtimestamp(v * 1e-3) if v else None
    return v

    
restutil.decorateModuleNamed(__name__)
logging.info('Models in %r decorated', __name__)
